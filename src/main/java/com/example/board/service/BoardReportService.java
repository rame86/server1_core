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
import com.example.board.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
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
    private final LikeRepository likeRepository;

    @Value("${file.upload.dir}") // [필수] 파일 삭제를 위한 경로 설정
    private String uploadDir;

    // --- [1. 신고 접수 로직] ---
    @Transactional
    public String reportBoard(Long boardId, Long memberId, String reason) {
        log.info("-----> [BoardReportService] 게시글 신고 시도: boardId={}, memberId={}", boardId, memberId);
        if (!boardRepository.existsById(boardId)) {
            throw new IllegalArgumentException("존재하지 않는 게시글입니다.");
        }
        if (reportRepository.existsByBoardIdAndMemberId(boardId, memberId)) {
            log.warn("-----> [BoardReportService] 이미 신고된 게시글: boardId={}, memberId={}", boardId, memberId);
            return "ALREADY_REPORTED";
        }
        BoardReport report = BoardReport.builder()
                .boardId(boardId)
                .memberId(memberId)
                .reason(reason != null && !reason.isEmpty() ? reason : "사유 없음")
                .status("PENDING")
                .createdAt(LocalDateTime.now()) // 생성 시간 명시적 주입
                .build();
        BoardReport saved = reportRepository.save(report);
        log.info("-----> [BoardReportService] 게시글 신고 DB 저장 완료: reportId={}", saved.getReportId());
        return "SUCCESS";
    }

    @Transactional
    public String reportComment(Long commentId, Long memberId, String reason) {
        log.info("-----> [BoardReportService] 댓글 신고 시도: commentId={}, memberId={}", commentId, memberId);
        
        if (!commentRepository.existsById(commentId)) {
            throw new IllegalArgumentException("존재하지 않는 댓글입니다.");
        }
        if (reportCommentRepository.existsByCommentIdAndMemberId(commentId, memberId)) {
            log.warn("-----> [BoardReportService] 이미 신고된 댓글: commentId={}, memberId={}", commentId, memberId);
            return "ALREADY_REPORTED";
        }
        ReportComment report = ReportComment.builder()
                .commentId(commentId)
                .memberId(memberId)
                .reason(reason != null && !reason.isEmpty() ? reason : "사유 없음")
                .status("PENDING")
                .createdAt(LocalDateTime.now()) // 생성 시간 명시적 주입
                .build();
                
        ReportComment saved = reportCommentRepository.save(report);
        log.info("-----> [BoardReportService] 댓글 신고 DB 저장 완료: reportId={}", saved.getReportId());
        return "SUCCESS";
    }

    // --- [2. 상태 변경 핵심 로직] ---
    @Transactional
    public void approveBoardReport(Long reportId, Long boardId) {
        BoardReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신고 내역이 없습니다. ID: " + reportId));
        
        report.setStatus("APPROVED");
        hideBoard(boardId);
        log.info("-----> [BoardReportService] 게시글 신고 승인 완료: reportId={}", reportId);
    }

    @Transactional
    public void approveCommentReport(Long reportId) {
        ReportComment report = reportCommentRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신고 내역이 없습니다. ID: " + reportId));
        
        report.setStatus("APPROVED");
        hideComment(report.getCommentId());
        log.info("-----> [BoardReportService] 댓글 신고 승인 완료: reportId={}", reportId);
    }
    
   // --- [관리자 전용: 게시글 영구 삭제] ---
    @Transactional
    public void hardDeleteBoard(Long boardId) {
        log.info("-----> [Admin] 게시글 영구 삭제 시작: boardId={}", boardId);
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 게시글이 없습니다. ID: " + boardId));

        // 1. 게시글 관련 신고 내역 삭제
        reportRepository.deleteByBoardId(boardId);
        
        // 2. 좋아요 내역 삭제
        likeRepository.deleteByBoardId(boardId);
        
        // 3. 해당 게시글에 달린 모든 댓글의 신고 내역 삭제
        List<Comment> comments = commentRepository.findByBoardId_BoardIdOrderByCreatedAtDesc(boardId);
        if (!comments.isEmpty()) {
            List<Long> commentIds = comments.stream().map(Comment::getCommentId).toList();
            // 댓글 신고 내역 먼저 삭제
            reportCommentRepository.deleteByCommentIdIn(commentIds);
            // 댓글 본체 삭제 (Batch 삭제보다는 안전하게 연관 관계를 고려하여 처리)
            commentRepository.deleteAll(comments); 
        }
       
        // 5. 물리 파일 삭제
        if (board.getStoredFilePath() != null) {
        deletePhysicalFile(uploadDir + File.separator + board.getStoredFilePath());
}
        // 6. 게시글 본체 삭제
        boardRepository.delete(board);
        log.info("-----> [Admin] 게시글 및 연관 데이터 완전 삭제 완료");
    }

    // --- [관리자 전용: 댓글 영구 삭제] ---
    @Transactional
    public void hardDeleteComment(Long commentId) {
        log.info("-----> [Admin] 댓글 영구 삭제 시작: commentId={}", commentId);
        
        if (!commentRepository.existsById(commentId)) {
            throw new IllegalArgumentException("삭제할 댓글이 없습니다. ID: " + commentId);
        }
        // 1. 해당 댓글과 관련된 신고 내역 삭제
        reportCommentRepository.deleteByCommentId(commentId);
        
        // 2. 댓글 본체 삭제
        commentRepository.deleteById(commentId);
        log.info("-----> [Admin] 댓글 및 연관 신고 내역 삭제 완료");
    }

    @Transactional
    public void deleteBoardReport(Long reportId) {
        if (!reportRepository.existsById(reportId)) {
            throw new IllegalArgumentException("삭제할 신고 내역이 없습니다. ID: " + reportId);
        }
        reportRepository.deleteById(reportId);
        log.info("-----> [BoardReportService] 신고 내역 삭제 완료: reportId={}", reportId);
    }
    
    @Transactional
    public void hideBoard(Long boardId) {
        boardRepository.findById(boardId).ifPresent(board -> {
            board.hideBoard(); // 엔티티 내부 메서드 호출
            resolveCommentReportsByBoardId(boardId);
        });
    }

    @Transactional
    public void hideComment(Long commentId) {
        commentRepository.findById(commentId).ifPresent(comment -> {
            comment.setStatus("HIDDEN");
        });
    }

    @Transactional
    public void resolveCommentReportsByBoardId(Long boardId) {
        List<Comment> comments = commentRepository.findByBoardId_BoardIdOrderByCreatedAtDesc(boardId);
        List<Long> commentIds = comments.stream().map(Comment::getCommentId).toList();

        if (!commentIds.isEmpty()) {
            reportCommentRepository.findByCommentIdIn(commentIds)
                    .forEach(report -> report.setStatus("RESOLVED"));
        }
    }

    // --- [3. 신고 목록 조회 로직] ---
    @Transactional(readOnly = true)
    public List<ReportBoardDTO> getBoardReportList() {
        // 대소문자 구분 없이 PENDING 상태만 조회하도록 equalsIgnoreCase 권장 (여기서는 문자열 리터럴 비교)
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
    // --- [5. 물리 파일 삭제 보조 메서드] ---
    private void deletePhysicalFile(String storedFileName) {
        if (storedFileName == null) return;
        try {
            // BoardService의 로직과 동일하게 경로를 조합합니다.
            File file = new File(uploadDir + File.separator + storedFileName);
            if (file.exists()) {
                if (file.delete()) {
                    log.info("-----> [파일삭제] 성공: {}", storedFileName);
                } else {
                    log.warn("-----> [파일삭제] 실패 (권한 문제 등): {}", storedFileName);
                }
            }
        } catch (Exception e) {
            log.error("-----> [파일삭제] 중 예외 발생: {}", e.getMessage());
        }
    }
}