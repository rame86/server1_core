package com.example.board.service;

import com.example.board.dto.ReportBoardDTO;
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

import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private final RabbitTemplate rabbitTemplate;

    // 게시글 신고 접수
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

    // 댓글 신고 접수
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

    // 신고 목록 조회
    @Transactional(readOnly = true)
    public List<ReportBoardDTO> getBoardReportList() {
        return reportRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::convertToReportDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportComment> getCommentReportList() {
        return reportCommentRepository.findAll();
    }

    // [관리자] 게시글 신고 승인 처리
    @Transactional
    public void approveBoardReport(Long reportId) {
        BoardReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 내역을 찾을 수 없습니다."));
        report.approve(); 

        Board board = boardRepository.findById(report.getBoardId())
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // RabbitMQ 메시지 전송
        rabbitTemplate.convertAndSend("board.exchange", "board.hide", board.getBoardId());
        
        log.info("관리자 승인: 신고 ID {} 승인. RabbitMQ로 게시글 ID {} 숨김 요청 전송", reportId, board.getBoardId());
    }

    // 댓글 신고 승인 처리
    @Transactional
    public void approveCommentReport(Long reportId) {
        ReportComment report = reportCommentRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("댓글 신고 내역을 찾을 수 없습니다."));
        
        report.approve();

        Comment comment = commentRepository.findById(report.getCommentId())
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        
        comment.hideComment();
        
        log.info("관리자 승인: 댓글 신고 ID {} 승인 완료", reportId);
    }

    // 내부 헬퍼 메서드: 게시글 제목을 포함하여 변환 [수정]
    private ReportBoardDTO convertToReportDTO(BoardReport report) {
        // 하드코딩 없이 Repository를 통해 실제 제목 조회
        String title = boardRepository.findById(report.getBoardId())
                .map(Board::getTitle)
                .orElse("삭제된 게시물");

        return ReportBoardDTO.builder()
                .reportId(report.getReportId())
                .boardId(report.getBoardId())
                .postTitle(title) // 추가된 제목 매핑
                .memberId(report.getMemberId())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}