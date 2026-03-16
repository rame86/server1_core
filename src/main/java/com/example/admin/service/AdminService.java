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
				.build()).toList();
	}
	
	
	
	//////////////정은언니 BOARD////////////////////////
	// 추가
	public List<ReportBoardDTO> getBoardReports() {

    
    
    
    
    
        // Board 서비스의 API 엔드포인트
        String url = "http://localhost:8080/board/admin/reports";
        
        try {
            log.info("-----> [AdminService] Board 서비스(Core)에 신고 내역 요청 중...");
            
            // RestTemplate을 사용하여 GET 요청을 보내고 배열 형태로 응답을 받음
            ReportBoardDTO[] response = restTemplate.getForObject(url, ReportBoardDTO[].class);
            
            if (response != null) {
                log.info("-----> [AdminService] 신고 내역 수신 성공: {}건", response.length);
                return Arrays.asList(response);
            }
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("-----> [AdminService] Board 서비스 호출 실패: {}", e.getMessage());
            // 서비스 호출 실패 시 빈 리스트 반환 (시스템 중단 방지)
            return Collections.emptyList
			();
        }
    }
// 2. [추가] 신고 승인 처리 (RabbitMQ 사용)
    @Transactional
    public void approveBoardReport(Long boardId) {
        log.info("-----> [AdminService2] 게시글 신고 승인 프로세스 시작. ID: {}", boardId);
        
        // 메시지 전송용 DTO 생성
        BoardReportMessageDTO message = new BoardReportMessageDTO(boardId, "HIDDEN");

        try {
            // RabbitMQ로 메시지 발행 (시큐리티 우회)
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME, 
                RabbitMQConfig.BOARD_REPORT_APPROVE_ROUTING_KEY, 
                message
            );
            log.info("-----> [AdminService] RabbitMQ 메시지 발행 완료 (RoutingKey: {})", 
                     RabbitMQConfig.BOARD_REPORT_APPROVE_ROUTING_KEY);
        } catch (Exception e) {
            log.error("-----> [AdminService] 메시지 발행 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("신고 승인 처리에 실패했습니다.");
        }
    }
}

