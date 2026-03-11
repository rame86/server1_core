package com.example.admin.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;      // 추가 필요
import java.util.Collections; // 추가 필요

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.admin.dto.AdminEventListDTO;
import com.example.admin.dto.ApprovalDTO;
import com.example.admin.dto.ReportBoardDTO;
import com.example.admin.entity.Approval;
import com.example.admin.repository.ApprovalRepository;
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
	private final RestTemplate restTemplate = new RestTemplate(); // 외부 호출용 도구

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
	@Transactional
	public void processApproval(ApprovalDTO dto, String routingKey, Long adminId) {		
		updateApprovalStatus(dto, adminId);
		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, dto);
		
		log.info("=====> [1서버] 처리 완료: ApprovalId={}, Status={}, RoutingKey={}", 
				dto.getApprovalId(), dto.getStatus(), routingKey);
	}
	
	private void updateApprovalStatus(ApprovalDTO dto, Long adminId) {
		Approval approval = approvalRepository.findById(dto.getApprovalId())
				.orElseThrow(() -> new RuntimeException("해당 신청 건을 찾을 수 없습니다."));
		
		approval.setStatus(dto.getStatus());
		approval.setAdminId(adminId);
		approval.setProcessedAt(LocalDateTime.now());
		
		if("REJECTED".equals(dto.getStatus())) {
			approval.setRejectionReason(dto.getRejectionReason());
		}
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
	
}
