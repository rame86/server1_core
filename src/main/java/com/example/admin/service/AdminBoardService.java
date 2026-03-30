package com.example.admin.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.admin.dto.ReportBoardDTO;
import com.example.admin.entity.Approval;
import com.example.admin.repository.ApprovalRepository;
import com.example.board.entity.Board;
import com.example.board.entity.BoardReport;
import com.example.board.entity.Comment;
import com.example.board.entity.ReportComment;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.CommentRepository;
import com.example.board.repository.ReportRepository;
import com.example.board.repository.ReportCommentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBoardService {

    private final ApprovalRepository approvalRepository;
    private final ReportRepository reportRepository;
    private final ReportCommentRepository reportCommentRepository;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;

    @Value("${service.board.url:}")
    private String boardServiceUrl;

    // --- [1. 게시글 신고 내역 조회] ---
    public List<ReportBoardDTO> getBoardReports() {
        log.info("-----> [AdminBoardService] 직접 DB에서 게시글 신고 내역 조회");
        try {
        return reportRepository.findAll().stream()
                .filter(report -> "PENDING".equals(report.getStatus()))
                // [추가] 실제 게시글이 존재하는지 확인 (없으면 목록에서 제외)
                .filter(report -> boardRepository.existsById(report.getBoardId())) 
                .map(this::convertToBoardReportDTO)
                .collect(Collectors.toList());
    } catch (Exception e) {
        log.error("-----> [AdminBoardService] 게시글 신고 조회 에러: {}", e.getMessage());
        return Collections.emptyList();
    }
}

    // --- [2. 댓글 신고 내역 조회] ---
    public List<ReportBoardDTO> getCommentReports() {
    log.info("-----> [AdminBoardService] 직접 DB에서 댓글 신고 내역 조회 및 유효성 검사");
    try {
        return reportCommentRepository.findAll().stream()
                .filter(report -> "PENDING".equals(report.getStatus()))
                // [추가] 실제 댓글이 존재하는지 확인 (없으면 목록에서 제외)
                .filter(report -> commentRepository.existsById(report.getCommentId()))
                .map(this::convertToCommentReportDTO)
                .collect(Collectors.toList());
    } catch (Exception e) {
        log.error("-----> [AdminBoardService] 댓글 신고 조회 에러: {}", e.getMessage());
        return Collections.emptyList();
    }
}

    // --- [DTO 변환 도우미 메서드] ---
    private ReportBoardDTO convertToBoardReportDTO(BoardReport report) {
        Board board = boardRepository.findById(report.getBoardId()).orElse(null);
        return ReportBoardDTO.builder()
                .reportId(report.getReportId())
                .boardId(report.getBoardId())
                .postTitle(board != null ? board.getTitle() : "삭제된 게시물")
                .content(board != null ? board.getContent() : "내용 없음")
                .memberId(report.getMemberId())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private ReportBoardDTO convertToCommentReportDTO(ReportComment report) {
        Comment comment = commentRepository.findById(report.getCommentId()).orElse(null);
        return ReportBoardDTO.builder()
                .reportId(report.getReportId())
                .boardId(null) // 댓글 신고이므로 boardId는 null 또는 별도 처리
                .postTitle("댓글 신고")
                .content(comment != null ? comment.getContent() : "삭제된 댓글")
                .memberId(report.getMemberId())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }

    /**
     * 3. 게시글 신고 승인 처리
     */
   @Transactional
public void approveBoardReport(Long reportId, Long boardId, Long adminId) {
    log.info("-----> [AdminBoardService] 게시글 승인 로직 시작! reportId: {}, boardId: {}", reportId, boardId);

    // 1) 신고서 확인
    BoardReport report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("해당 신고 내역을 찾을 수 없습니다."));

    // 2) 실제 게시글 조회 및 처리
    Board board = boardRepository.findById(boardId).orElse(null);

    if (board == null) {
        // [핵심 추가] 게시글이 이미 DB에 없다면 신고 내역을 삭제하고 종료
        log.warn("-----> [AdminBoardService] 대상 게시글(ID: {})이 이미 존재하지 않습니다. 신고 내역을 삭제합니다.", boardId);
        reportRepository.delete(report); 
        return; // 로직 종료
    }

    // 3) 게시글이 존재한다면 정상적으로 승인/숨김 처리
    report.approve(); 
    board.hideBoard(); 

    // 4) 관리자 승인 이력 저장
    saveApprovalLog(reportId, adminId, "게시글 신고 승인 (Board ID: " + boardId + ")");
    
    log.info("-----> [AdminBoardService] 게시글 승인 및 HIDDEN 처리 완료");
}

    /**
     * 4. 댓글 신고 승인 처리
     */
    @Transactional
    public void approveCommentReport(Long reportId, Long adminId) {
        log.info(">>>> [AdminBoardService] 댓글 승인 로직 시작! reportId: {}, adminId: {}", reportId, adminId);

        // 1. 신고 내역 조회
        ReportComment report = reportCommentRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 내역을 찾을 수 없습니다."));

        // 2. 대상 댓글 조회
        Long commentId = report.getCommentId();
        Comment comment = commentRepository.findById(commentId).orElse(null);

        // [핵심 추가] 댓글이 이미 삭제되어 DB에 없는 경우
        if (comment == null) {
            log.warn(">>>> [AdminBoardService] 대상 댓글(ID: {})이 이미 존재하지 않습니다. 신고 내역을 정리합니다.", commentId);
            reportCommentRepository.delete(report); // 신고 내역 삭제
            return; // 로직 종료 (에러 없이 리스트에서 사라짐)
        }

        // 3. 댓글이 존재하면 정상 승인 처리
        report.approve(); // 신고서 상태 APPROVED로 변경
        comment.hideComment(); // 댓글 상태 HIDDEN으로 변경

        // 4. 승인 이력 저장
        saveApprovalLog(reportId, adminId, "댓글 신고 승인 (Comment ID: " + commentId + ")");
        
        log.info(">>>> [AdminBoardService] 댓글 승인 및 HIDDEN 처리 완료");
    }

    // 5. 신고 내역 삭제
    @Transactional
    public void deleteBoardReport(Long reportId) {
        log.info("-----> [AdminBoardService] 신고 내역 삭제 요청 수신: reportId={}", reportId);

        // 1. 게시글 신고 테이블에서 확인
        BoardReport report = reportRepository.findById(reportId).orElse(null);
        if (report != null) {
            reportRepository.delete(report);
            log.info("-----> [AdminBoardService] 게시글 신고 내역(ID: {}) 삭제 완료", reportId);
            return;
        }

        // 2. 댓글 신고 테이블에서 확인
        ReportComment commentReport = reportCommentRepository.findById(reportId).orElse(null);
        if (commentReport != null) {
            reportCommentRepository.delete(commentReport);
            log.info("-----> [AdminBoardService] 댓글 신고 내역(ID: {}) 삭제 완료", reportId);
            return;
        }

        // 3. 둘 다 없을 경우에만 예외 발생
        log.warn("-----> [AdminBoardService] 삭제할 신고 내역이 없습니다. ID: {}", reportId);
    }

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