package com.example.admin.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.config.RabbitMQConfig;
import com.example.admin.dto.BoardReportMessageDTO;
import com.example.admin.dto.ReportBoardDTO;
import com.example.admin.entity.Approval;
import com.example.admin.repository.ApprovalRepository;
import com.example.board.entity.BoardReport;
import com.example.board.entity.ReportComment; // 댓글 신고 엔티티 추가
import com.example.board.repository.ReportRepository;
import com.example.board.repository.ReportCommentRepository; // 댓글 신고 레포지토리 추가
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBoardService {

    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;
    private final ApprovalRepository approvalRepository;
    private final ReportRepository reportRepository; // 신고 상태 변경을 위해 주입
    private final ReportCommentRepository reportCommentRepository; // 주입 추가

    @Value("${service.board.url:http://localhost:8080}")
    private String boardServiceUrl;

    // 신고 내역 조회
    public List<ReportBoardDTO> getBoardReports() {
        String url = boardServiceUrl + "/board/admin/reports/boards";
        try {
            ReportBoardDTO[] response = restTemplate.getForObject(url, ReportBoardDTO[].class);
            return response != null ? Arrays.asList(response) : Collections.emptyList();
        } catch (Exception e) {
            log.error("-----> [AdminBoardService] 내역 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // 댓글 신고 내역 조회
    public List<ReportBoardDTO> getCommentReports() {
        String url = boardServiceUrl + "/board/admin/reports/comments"; // Board 서비스의 댓글 신고 API 호출
        try {
            ReportBoardDTO[] response = restTemplate.getForObject(url, ReportBoardDTO[].class);
            return response != null ? Arrays.asList(response) : Collections.emptyList();
        } catch (Exception e) {
            log.error("-----> [AdminBoardService] 댓글 내역 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // 게시글 신고 승인
    @Transactional
    public void approveBoardReport(Long reportId, Long boardId, Long adminId) {
        log.info("-----> [AdminBoardService] 승인 프로세스 시작. 리포트ID: {}, 게시글ID: {}", reportId, boardId);

        // 1) [DB 수정] 신고 내역(board_report)의 상태를 APPROVED로 변경
        BoardReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("해당 신고 내역을 찾을 수 없습니다."));
        
        report.approve(); // status = "APPROVED"
        reportRepository.save(report); 
        log.info("-----> [AdminBoardService] 신고 상태 변경 완료 (ID: {})", reportId);

        // 2) [DB 저장] 관리자 서비스의 승인 이력 저장
        Approval approval = Approval.builder()
                .category("BOARD_REPORT")
                .targetId(reportId)
                .status("CONFIRMED")
                .adminId(adminId)
                .title("게시글 신고 승인 (Board ID: " + boardId + ")")
                .processedAt(LocalDateTime.now())
                .build();
        approvalRepository.save(approval);

        // 3) [RabbitMQ] 메시지 발행 (상수 사용 및 동적 boardId 전달)
        BoardReportMessageDTO message = new BoardReportMessageDTO(boardId);
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME, 
                RabbitMQConfig.BOARD_REPORT_APPROVE_ROUTING_KEY, 
                message
            );
            log.info("-----> [AdminBoardService] RabbitMQ 메시지 발행 성공: {}", message);
        } catch (Exception e) {
            log.error("-----> [AdminBoardService] 메시지 발행 실패: {}", e.getMessage());
            throw new RuntimeException("메시지 발행 실패로 승인 처리가 중단되었습니다.");
        }
    }

    // 댓글 신고 승인
    @Transactional
    public void approveCommentReport(Long reportId, Long adminId) {
        log.info("-----> [AdminBoardService] 댓글 승인 시작. 리포트ID: {}", reportId);

        ReportComment report = reportCommentRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("해당 댓글 신고 내역을 찾을 수 없습니다. ID: " + reportId));
        
        report.approve(); 
        reportCommentRepository.save(report);

        String url = boardServiceUrl + "/board/admin/reports/comments/" + reportId + "/approve";
        try {
            restTemplate.put(url, null);
            log.info("-----> [AdminBoardService] Board 서비스 통신 성공");
        } catch (Exception e) {
            log.error("-----> [AdminBoardService] Board 서비스 통신 실패: {}", e.getMessage());
            throw new RuntimeException("댓글 서비스와 통신할 수 없습니다.");
        }

        // [핵심 수정] 아래 정의된 saveApprovalLog의 파라미터 개수에 맞게 호출 (3개 전달)
        saveApprovalLog(reportId, adminId, "댓글 신고 승인 (Report ID: " + reportId + ")");
    }


    //신고 내역 삭제
    public void deleteBoardReport(Long reportId) {
        String url = boardServiceUrl + "/board/admin/reports/boards/" + reportId;
        try {
            log.info("-----> [AdminBoardService] Board 서비스로 신고 삭제 요청: reportId={}", reportId);
            restTemplate.delete(url);
            log.info("-----> [AdminBoardService] 신고 삭제 성공");
        } catch (Exception e) {
            log.error("-----> [AdminBoardService] 신고 삭제 실패: {}", e.getMessage());
            throw new RuntimeException("게시판 서비스와의 통신 중 오류가 발생했습니다.");
        }
    }
    // 공통 이력 저장 메서드 (중복 제거)
    private void saveApprovalLog(Long targetId, Long adminId, String title) {
        Approval approval = Approval.builder()
                .category("REPORT")
                .targetId(targetId)
                .status("CONFIRMED")
                .adminId(adminId)
                .title(title)
                .processedAt(LocalDateTime.now())
                .build();
        approvalRepository.save(approval);
    }
}