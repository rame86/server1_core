package com.example.admin.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.admin.dto.BoardReportMessageDTO;
import com.example.admin.dto.ReportBoardDTO;
import com.example.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBoardService {

    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;

    // application.yml 또는 환경변수에서 board 서비스의 기본 URL을 가져옵니다.
    @Value("${service.board.url:http://localhost:8080}")
    private String boardServiceUrl;

    /**
     * 1. 게시글 신고 내역 조회 (HTTP 통신)
     */
    public List<ReportBoardDTO> getBoardReports() {
        // [수정] 하드코딩된 localhost 대신 주입받은 boardServiceUrl 변수를 사용합니다.
        String url = boardServiceUrl + "/board/admin/reports";
        
        try {
            log.info("-----> [AdminBoardService] Board 서비스(Core)에 신고 내역 요청 중...");
            
            // RestTemplate을 사용하여 GET 요청을 보내고 배열 형태로 응답을 받음
            ReportBoardDTO[] response = restTemplate.getForObject(url, ReportBoardDTO[].class);
            
            if (response != null) {
                log.info("-----> [AdminBoardService] 신고 내역 수신 성공: {}건", response.length);
                return Arrays.asList(response);
            }
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("-----> [AdminBoardService] Board 서비스 호출 실패: {}", e.getMessage());
            // 시스템 중단 방지를 위해 빈 리스트 반환
            return Collections.emptyList();
        }
    }

    /**
     * 2. 게시글 신고 승인 처리 (RabbitMQ 사용)
     */
    @Transactional
    public void approveBoardReport(Long boardId) {
        log.info("-----> [AdminBoardService] 게시글 신고 승인 프로세스 시작. ID: {}", boardId);
        
        // 메시지 전송용 DTO 생성 (게시글을 숨김 상태로 변경하기 위한 목적)
        BoardReportMessageDTO message = new BoardReportMessageDTO(boardId, "HIDDEN");

        try {
            // RabbitMQ로 메시지 발행
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME, 
                RabbitMQConfig.BOARD_REPORT_APPROVE_ROUTING_KEY, 
                message
            );
            
            log.info("-----> [AdminBoardService] RabbitMQ 메시지 발행 완료 (RoutingKey: {})", 
                     RabbitMQConfig.BOARD_REPORT_APPROVE_ROUTING_KEY);
                     
        } catch (Exception e) {
            log.error("-----> [AdminBoardService] 메시지 발행 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("신고 승인 처리에 실패했습니다.");
        }
    }
}