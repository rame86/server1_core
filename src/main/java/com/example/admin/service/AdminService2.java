package com.example.admin.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Collections;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.admin.client.PayClient;
import com.example.admin.dto.AdminEventListDTO;
import com.example.admin.dto.ApprovalDTO;
import com.example.admin.dto.ReportBoardDTO;
import com.example.admin.dto.BoardReportMessageDTO; // 추가됨
import com.example.admin.dto.EventResultDTO;
import com.example.admin.dto.SettlementDashboardResponse;
import com.example.admin.entity.Approval;
import com.example.admin.repository.ApprovalRepository;
import com.example.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService2 {

    private final ApprovalRepository approvalRepository;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;
    private final PayClient payClient;
    private final RestTemplate restTemplate;

    @Transactional
    public void processApproval(ApprovalDTO dto, String routingKey, Long adminId) {     
        Long eventId = updateApprovalStatus(dto, adminId);
        if(dto instanceof EventResultDTO eventResultDto) {
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
        
        if("REJECTED".equals(dto.getStatus())) {
            approval.setRejectionReason(dto.getRejectionReason());
        }
        
        return approval.getTargetId();
    }
    
    @Transactional(readOnly = true)
    public List<AdminEventListDTO> getAllEvents() {
        List<Approval> approvals = approvalRepository.findAllByOrderByCreatedAtDesc();
        
        return approvals.stream().map(approval -> {
            String redisKey = "event:stock:" + approval.getTargetId();
            String stockVal = redisTemplate.opsForValue().get(redisKey);
            int currentStock = (stockVal != null) ? Integer.parseInt(stockVal) : 0;
            
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
                    .stock(currentStock)
                    .build();
        }).collect(Collectors.toList());
    }
    
    public SettlementDashboardResponse getDashboardData(String yearMonth) {
        if(yearMonth == null || yearMonth.isBlank()) {
            yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        return payClient.getDashboardData(yearMonth);
    }
    
    ////////////// BOARD 관련 (신고 내역 조회 및 승인) ////////////////////////
    
    // 1. 신고 내역 조회 (기존 유지)
    public List<ReportBoardDTO> getBoardReports() {
        String url = "http://localhost:8080/board/admin/reports";
        try {
            log.info("-----> [AdminService2] Board 서비스(Core)에 신고 내역 요청 중...");
            ReportBoardDTO[] response = restTemplate.getForObject(url, ReportBoardDTO[].class);
            
            if (response != null) {
                log.info("-----> [AdminService2] 신고 내역 수신 성공: {}건", response.length);
                return Arrays.asList(response);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("-----> [AdminService2] Board 서비스 호출 실패: {}", e.getMessage());
            return Collections.emptyList();
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