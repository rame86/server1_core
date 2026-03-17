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
import org.springframework.web.reactive.function.client.WebClient;

import com.example.admin.dto.AdminEventListDTO;
import com.example.admin.dto.ApprovalDTO;
import com.example.admin.dto.ArtistAccountResponse;
import com.example.admin.dto.ArtistApprovedEvent;
import com.example.admin.dto.ArtistResponseDTO;
import com.example.admin.dto.ArtistResultDTO;
import com.example.admin.dto.EventResultDTO;
import com.example.admin.dto.SettlementDashboardResponse;
import com.example.admin.entity.Approval;
import com.example.admin.repository.ApprovalRepository;
import com.example.artist.dto.PaymentRequestDTO;
import com.example.artist.dto.PaymentResponseDTO;
import com.example.artist.entity.Artist;
import com.example.artist.repository.ArtistRepository;
import com.example.config.RabbitMQConfig;
import com.example.member.domain.Member;
import com.example.member.repository.MemberRepository;

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
	private final WebClient webClient;
	private final ArtistRepository artistRepository;

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
	public List<ArtistResultDTO> getPendingArtistList(String category, String status) {
		List<Approval> entityList = approvalRepository.findByCategoryAndStatus(category, status);
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
		
		if(!"PENDING".equals(approval.getStatus())) {
			throw new IllegalStateException("이미 처리되었거나 취소된 신청건입니다.");
		}
		
		if(artistRepository.existsById(approval.getArtistId())) {
			throw new IllegalStateException("이미 아티스트로 등록된 회원입니다.");
		}
		
		approval.setStatus("CONFIRMED");
		approval.setAdminId(adminId);
		approval.setProcessedAt(LocalDateTime.now());
		
		// MEMBER테이블 권한 변경
		Member member = memberRepository.findById(approval.getArtistId())
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
		member.setRole("ARTIST");
		member.setStatus("ACTIVE");
		
		// ARTIST테이블 INSERT
		Artist artist = new Artist();
		artist.setMember(member);
		artist.setStageName(approval.getRequesterName());
		artist.setCategory(approval.getSubCategory());
		artist.setDescription(approval.getDescription());
		artistRepository.save(artist);
		
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
		approval.setProcessedAt(LocalDateTime.now());
		
		Member member = memberRepository.findById(approval.getArtistId())
				.orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
		if("PENDING".equals(member.getStatus())) {
			member.setStatus("ACTIVE");
			log.info("-----> [상태 복구] MemberID: {} 님이 일반 유저(ACTIVE)로 전환되었습니다.", member.getMemberId());
		}
		
		log.info("-----> [거절 완료] ID: {}, 사유: {}", dto.getApprovalId(), dto.getRejectionReason());
	}
	
	// artist 승인된 리스트
	@Transactional(readOnly = true)
	public List<ArtistResponseDTO> getActiceArtistList(String category, String status) {
		List<Approval> entityList = approvalRepository.findByCategoryAndStatus(category, status);
		
		return entityList.stream().map(entity -> ArtistResponseDTO.builder()
				.approvalId(entity.getApprovalId())
				.artistId(entity.getArtistId())
                .artistName(entity.getRequesterName())
                .category(entity.getSubCategory())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt().toString())
                .processedAt(entity.getProcessedAt() != null ? entity.getProcessedAt().toString() : null)
                .adminId(entity.getAdminId())
                .build())
				.collect(Collectors.toList());
	}
	
	// artist 상세 보기
	public ArtistResponseDTO getArtistDetail(Long approvalId, Long artistId) {
		
		// 1서버 정보 호출
		Approval approval = approvalRepository.findById(approvalId)
				.orElseThrow(() -> new IllegalArgumentException("해당 기록이 없습니다. ID: " + approvalId));
		Member member = memberRepository.findById(artistId)
				.orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다. ID: " + artistId));
		
		// 2서버 WebClient 호출
		ArtistAccountResponse paymentData = webClient.get()
				.uri("http://localhost:8083/wallet/artist/{artistId}", artistId)
				.retrieve()
				.bodyToMono(ArtistAccountResponse.class)
				.block();
		
		return ArtistResponseDTO.builder()
				.approvalId(approval.getApprovalId())
				.artistId(member.getMemberId())
				.artistName(member.getName())
				.category(approval.getCategory())
				.description(approval.getDescription())
				.createdAt(approval.getCreatedAt().toString())
				.status(approval.getStatus())
				.totalBalance(paymentData.getTotalBalance())
				.withdrawableBalance(paymentData.getWithdrawableBalance())
				.rejectionReason(approval.getRejectionReason())
				.adminId(approval.getAdminId())
				.processedAt(approval.getProcessedAt() != null ? approval.getProcessedAt().toString() : "처리 대기중")
				.build();
		
	}
	
}
