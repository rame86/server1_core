package com.example.board.service;

import com.example.admin.dto.ReportBoardDTO;
import com.example.board.entity.Board;
import com.example.board.entity.BoardReport;
import com.example.board.entity.Comment;
import com.example.board.entity.ReportComment;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.CommentRepository;
import com.example.board.repository.ReportCommentRepository;
import com.example.board.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardReportService {

    private final ReportRepository reportRepository;
    private final ReportCommentRepository reportCommentRepository;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;

    // --- [1. 신고 접수 로직] --- (사용자가 호출)
    @Transactional
    public String reportBoard(Long boardId, Long memberId, String reason) {
        if (!boardRepository.existsById(boardId)) throw new IllegalArgumentException("존재하지 않는 게시글입니다.");
        if (reportRepository.existsByBoardIdAndMemberId(boardId, memberId)) return "ALREADY_REPORTED";

        BoardReport report = BoardReport.builder()
                .boardId(boardId)
                .memberId(memberId)
                .reason(reason != null ? reason : "사유 없음")
                .status("PENDING")
                .build();
        reportRepository.save(report);
        return "SUCCESS";
    }

    @Transactional
    public String reportComment(Long commentId, Long memberId, String reason) {
        if (!commentRepository.existsById(commentId)) throw new IllegalArgumentException("댓글 없음");
        if (reportCommentRepository.existsByCommentIdAndMemberId(commentId, memberId)) return "ALREADY_REPORTED";
        
        reportCommentRepository.save(ReportComment.builder()
                .commentId(commentId)
                .memberId(memberId)
                .reason(reason != null ? reason : "사유 없음")
                .status("PENDING")
                .build());
        return "SUCCESS";
    }

    // --- [2. 상태 변경 핵심 로직] --- (Admin API에서 호출)

    // [추가] 게시글 신고 승인 처리
    @Transactional
    public void approveBoardReport(Long reportId, Long boardId) {
        BoardReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신고 내역이 없습니다. ID: " + reportId));
        
        // 1. 신고 상태를 승인(APPROVED)으로 변경
        report.setStatus("APPROVED");
        
        // 2. 실제 게시글 숨김 처리
        hideBoard(boardId);
        
        log.info("-----> [BoardReportService] 게시글 신고 승인 및 숨김 완료: reportId={}, boardId={}", reportId, boardId);
    }

    // 댓글 신고 승인 처리
    @Transactional
    public void approveCommentReport(Long reportId) {
        ReportComment report = reportCommentRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신고 내역이 없습니다. ID: " + reportId));
        
        // 1. 신고 상태를 승인(APPROVED)으로 변경
        report.setStatus("APPROVED");
        
        // 2. 실제 댓글 숨김 처리
        hideComment(report.getCommentId());
        
        log.info("-----> [BoardReportService] 댓글 신고 승인 및 숨김 완료: reportId={}", reportId);
    }
    
    // 신고 내역 삭제 처리
    @Transactional
    public void deleteBoardReport(Long reportId) {
        if (!reportRepository.existsById(reportId)) {
            throw new IllegalArgumentException("삭제할 신고 내역이 없습니다. ID: " + reportId);
        }
        reportRepository.deleteById(reportId);
        log.info("-----> [BoardReportService] 신고 내역 삭제 완료: reportId={}", reportId);
    }
    
    // 게시글을 숨김 처리하고 관련 신고를 완료 상태로 변경
    @Transactional
    public void hideBoard(Long boardId) {
        boardRepository.findById(boardId).ifPresent(board -> {
            board.hideBoard(); // status = "HIDDEN"
            log.info("-----> [BoardReportService] 게시글 숨김 완료: {}", boardId);
            
            // 해당 게시글에 달린 댓글들의 모든 신고 내역도 정리
            resolveCommentReportsByBoardId(boardId);
        });
    }

    // 댓글을 숨김 처리
    @Transactional
    public void hideComment(Long commentId) {
        commentRepository.findById(commentId).ifPresent(comment -> {
            comment.setStatus("HIDDEN");
            log.info("-----> [BoardReportService] 댓글 숨김 완료: {}", commentId);
        });
    }

    // 특정 게시글의 모든 댓글 신고를 정리
    @Transactional
    public void resolveCommentReportsByBoardId(Long boardId) {
        List<Comment> comments = commentRepository.findByBoardId_BoardIdOrderByCreatedAtDesc(boardId);
        List<Long> commentIds = comments.stream().map(Comment::getCommentId).toList();

        if (!commentIds.isEmpty()) {
            reportCommentRepository.findByCommentIdIn(commentIds)
                    .forEach(report -> report.setStatus("RESOLVED"));
            log.info("-----> [BoardReportService] 게시글 {}의 댓글 신고 {}건 정리 완료", boardId, commentIds.size());
        }
    }

    // --- [3. 신고 목록 조회 로직] --- (Admin 용)
    @Transactional(readOnly = true)
    public List<ReportBoardDTO> getBoardReportList() {
        return reportRepository.findAll().stream()
                .filter(report -> "PENDING".equals(report.getStatus())) 
                .map(this::convertToReportDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportBoardDTO> getCommentReportList() {
        return reportCommentRepository.findAll().stream()
                .filter(report -> "PENDING".equals(report.getStatus()))
                .map(this::convertToCommentReportDTO)
                .collect(Collectors.toList());
    }

    // --- [4. 기타 DTO 변환] ---
    private ReportBoardDTO convertToReportDTO(BoardReport report) {
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
                .boardId(null)
                .postTitle("댓글 신고")
                .content(comment != null ? comment.getContent() : "삭제된 댓글")
                .memberId(report.getMemberId())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}