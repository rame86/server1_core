package com.example.admin.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate; // 추가 필요
import org.springframework.data.redis.core.StringRedisTemplate; // 추가 필요
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.dto.AdminEventListDTO;
import com.example.admin.dto.ApprovalDTO;
import com.example.admin.dto.ArtistApprovedEvent;
import com.example.admin.dto.ArtistResultDTO;
import com.example.admin.dto.EventResultDTO;
import com.example.admin.dto.SettlementDashboardResponse;
import com.example.admin.entity.Approval;
import com.example.admin.repository.ApprovalRepository;
import com.example.artist.dto.PaymentRequestDTO;
import com.example.artist.dto.PaymentResponseDTO;
import com.example.config.RabbitMQConfig;
import com.example.member.repository.MemberRepository;
import com.example.member.domain.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

	private final ApprovalRepository approvalRepository;
	private final RabbitTemplate rabbitTemplate;
	private final StringRedisTemplate redisTemplate;
	private final Map<String, CompletableFuture<SettlementDashboardResponse>> pendingRequests = new ConcurrentHashMap<>();
	private static final int MQ_TIMEOUT_SECONDS = 1;
	private final MemberRepository memberRepository;

	@Transactional
	public void processApproval(ApprovalDTO dto, String routingKey, Long adminId) {
		Long eventId = updateApprovalStatus(dto, adminId);
		if (dto instanceof EventResultDTO eventResultDto) {
			eventResultDto.setApprovalId(eventId);
		}
		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, dto);
	}

	private Long updateApprovalStatus(ApprovalDTO dto, Long adminId) {
		Approval approval = approvalRepository.findById(dto.getApprovalId())
				.orElseThrow(() -> new RuntimeException("해당 신청 건을 찾을 수 없습니다."));

		approval.setStatus(dto.getStatus());
		approval.setAdminId(adminId);
		approval.setProcessedAt(LocalDateTime.now());

		if ("REJECTED".equals(dto.getStatus())) {
			approval.setRejectionReason(dto.getRejectionReason());
		}

		return approval.getTargetId();
	}

	@Transactional(readOnly = true)
	public List<AdminEventListDTO> getAllEvents() {
		List<Approval> approvals = approvalRepository.findAllByOrderByCreatedAtDesc();

		return approvals.stream().map(approval -> {
			// redis에서 실시간 잔여석 조회하기
			String redisKey = "event:stock:" + approval.getTargetId();
			String stockVal = redisTemplate.opsForValue().get(redisKey);
			int currentStock = (stockVal != null) ? Integer.parseInt(stockVal) : 0;

			// DTO로 변환
			return AdminEventListDTO.builder()
					.approvalId(approval.getApprovalId())
					.targetId(approval.getTargetId())
					.title(approval.getTitle())
					.status(approval.getStatus())
					.category(approval.getCategory())
					.location(approval.getLocation())
					.price(approval.getPrice())
					.eventStartDate(approval.getEventStartDate())
					.createdAt(approval.getCreatedAt())
					.stock(currentStock) // Redis 데이터 합체
					.build();
		}).collect(Collectors.toList());
	}

	public SettlementDashboardResponse requestDashboardData() {
		String requestId = "ADMIN_SETTLEMENT_REQ";
		CompletableFuture<SettlementDashboardResponse> future = new CompletableFuture<>();
		pendingRequests.put(requestId, future);
		
		PaymentRequestDTO dto = PaymentRequestDTO.builder()
				.type("ADMIN_SETTLEMENT")
				.replyRoutingKey(RabbitMQConfig.ADMIN_PAY_RES_ROUTING_KEY)
				.build();
		rabbitTemplate.convertAndSend(
				RabbitMQConfig.EXCHANGE_NAME,
				RabbitMQConfig.PAY_REQ_ROUTING_KEY,
				dto);
		
		try {
			return future.get(MQ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (Exception e) {
			pendingRequests.remove(requestId);
			return new SettlementDashboardResponse(null, null);
		}
	}

	@RabbitListener(queues = "admin.pay.res.core.queue")
	public void receiveDashboardData(PaymentResponseDTO<SettlementDashboardResponse> response) {
		log.info("=====> [RabbitMQ 비동기 응답 수신] 상세 데이터: {}", response);
		
        if ("COMPLETE".equals(response.status())) {
            CompletableFuture<SettlementDashboardResponse> future = pendingRequests.remove("ADMIN_SETTLEMENT_REQ");
            if (future != null) {
                log.info("=====> [AdminService] 드디어 진짜 데이터를 찾음");
                future.complete(response.payload()); // 여기서 기다리던 스레드가 깨어남!
            }
        } else {
            log.info("=====> [AdminService] 접수 알림(PROCESSING) 더 기다림.");
        }
	}

	// artist 승인 대기중인 목록
	public List<ArtistResultDTO> getPendingArtistList(String artist, String status) {
		List<Approval> entityList = approvalRepository.findByCategoryAndStatus(artist, status);
		return entityList.stream().map(entity -> ArtistResultDTO.builder()
				.approvalId(entity.getApprovalId())
				.artistName(entity.getRequesterName())
				.subCategory(entity.getSubCategory())
				.description(entity.getDescription())
				.imageUrl(entity.getImageUrl())
				.status(entity.getStatus())
				.createdAt(entity.getCreatedAt().toString())
				.rejectionReason(entity.getRejectionReason())
				.build())
				.collect(Collectors.toList());
	}
	
	// artist 승인
	@Transactional
	public void confirmArtist(ArtistResultDTO dto, Long adminId) {
		// 신청서 승인 상태로 변경
		Approval approval = approvalRepository.findById(dto.getApprovalId())
				.orElseThrow(() -> new IllegalArgumentException("신청서가 없습니다."));
		approval.setStatus("CONFIRMED");
		approval.setAdminId(adminId);
		
		// MEMBER테이블 권한 변경
		Member member = memberRepository.findById(approval.getArtistId())
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
		member.setRole("ARTIST");
		member.setStatus("ACTIVE");
		
		// 2서버로 메세지 발송
		ArtistApprovedEvent event = ArtistApprovedEvent.builder()
				.memberId(member.getMemberId())
				.artistName(approval.getRequesterName())
				.type("ARTIST_APPROVE")
				.build();
				
		rabbitTemplate.convertAndSend(
				RabbitMQConfig.EXCHANGE_NAME,
				RabbitMQConfig.PAY_REQ_ROUTING_KEY, 
				event);
		
		log.info("-----> [ARTIST 완료] 1서버 DB 갱신, 메세지 전송 완료");
	}
	
	// artist 거절
	@Transactional
	public void rejectArtist(ArtistResultDTO dto, Long adminId) {
		Approval approval = approvalRepository.findById(dto.getApprovalId())
	            .orElseThrow(() -> new IllegalArgumentException("신청서가 없습니다."));
		
		approval.setStatus("REJECTED");
		approval.setRejectionReason(dto.getRejectionReason());
		approval.setAdminId(adminId);
		
		log.info("-----> [거절 완료] ID: {}, 사유: {}", dto.getApprovalId(), dto.getRejectionReason());
	}
	
}
