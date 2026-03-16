package com.example.admin.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate; // 추가 필요
import org.springframework.data.redis.core.StringRedisTemplate; // 추가 필요
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.admin.client.PayClient;
import com.example.admin.dto.AdminEventListDTO;
import com.example.admin.dto.ApprovalDTO;
import com.example.admin.dto.ArtistResultDTO;
import com.example.admin.dto.BoardReportMessageDTO;
import com.example.admin.dto.ReportBoardDTO;

import com.example.admin.dto.EventResultDTO;
import com.example.admin.dto.ReportBoardDTO;
import com.example.admin.dto.SettlementDashboardResponse;
import com.example.admin.entity.Approval;
import com.example.admin.repository.ApprovalRepository;
import com.example.artist.dto.PaymentRequestDTO;
import com.example.artist.dto.PaymentResponseDTO;
import com.example.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

	private final ApprovalRepository approvalRepository;
	private final RabbitTemplate rabbitTemplate;
	private final StringRedisTemplate redisTemplate;
	private final PayClient payClient;
	private final RestTemplate restTemplate; // 외부 호출용 도구

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

	// 1. 기존 메서드는 요청만 보내고 끝냅니다 (return void)
	public void requestDashboardData() {
		PaymentRequestDTO dto = PaymentRequestDTO.builder()
				.type("ADMIN_SETTLEMENT")
				.replyRoutingKey(RabbitMQConfig.ADMIN_PAY_RES_ROUTING_KEY)
				.build();

		// convertSendAndReceive 대신 convertAndSend 사용
		rabbitTemplate.convertAndSend(
				RabbitMQConfig.EXCHANGE_NAME,
				RabbitMQConfig.PAY_REQ_ROUTING_KEY,
				dto);
	}

	@RabbitListener(queues = "admin.pay.res.core.queue")
	public void receiveDashboardData(PaymentResponseDTO<SettlementDashboardResponse> response) {
		log.info("=====> [RabbitMQ 비동기 응답 수신] 상세 데이터: {}", response);
	}

	////////////// 정은언니 BOARD////////////////////////

	// public List<ArtistResultDTO> getPendingArtistList(String artist, String
	// status) {
	// List<Approval> entityList =
	// approvalRepository.findByCategoryAndStatus(artist, status);
	// return entityList.stream().map(entity -> ArtistResultDTO.builder()
	// .approvalId(entity.getApprovalId())
	// .artistName(entity.getRequesterName())
	// .subCategory(entity.getSubCategory())
	// .description(entity.getDescription())
	// .imageUrl(entity.getImageUrl())
	// .status(entity.getStatus())
	// .createdAt(entity.getCreatedAt().toString())
	// .rejectionReason(entity.getRejectionReason())
	// .build()).toList();
	// }
	public List<ArtistResultDTO> getPendingArtistList(String artist, String status) {
		List<Approval> entityList = approvalRepository.findByCategoryAndStatus(artist, status);

		return entityList.stream()
				.map(entity -> ArtistResultDTO.builder()
						.approvalId(entity.getApprovalId())
						.artistName(entity.getRequesterName())
						.subCategory(entity.getSubCategory())
						.description(entity.getDescription())
						.imageUrl(entity.getImageUrl())
						.status(entity.getStatus())
						// Null 체크 추가 및 명시적 변환
						.createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
						.rejectionReason(entity.getRejectionReason())
						.build())
				.collect(Collectors.toList()); // .toList() 대신 사용
	}

	
}
