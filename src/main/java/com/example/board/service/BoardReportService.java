package com.example.board.service;

import com.example.admin.dto.BoardReportMessageDTO;
import com.example.board.dto.CommentReportRequestDTO; // 신고 전용 DTO
import com.example.board.dto.ReportBoardDTO;
import com.example.board.entity.Board;
import com.example.board.entity.BoardReport;
import com.example.board.entity.Comment;
import com.example.board.entity.ReportComment;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.CommentRepository;
import com.example.board.repository.ReportCommentRepository;
import com.example.board.repository.ReportRepository;
import com.example.config.RabbitMQConfig;

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

    // --- [1. 신고 접수 로직] ---
    
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

    // --- [2. 신고 목록 조회 로직 (어드민용)] ---

    // 게시글 신고 목록 조회 (PEDING 상태만)
   @Transactional(readOnly = true)
    public List<ReportBoardDTO> getBoardReportList() {
    log.info("-----> [BoardReportService] 대기 중인 신고 목록만 조회합니다.");
    return reportRepository.findAll().stream()
            // 2. [중요] 상태가 "PENDING"인 것만 필터링합니다! 
            .filter(report -> "PENDING".equals(report.getStatus())) 
            .map(this::convertToReportDTO)
            .collect(Collectors.toList());
}
   @Transactional(readOnly = true)
    public List<ReportBoardDTO> getCommentReportList() {
        log.info("-----> [BoardReportService] 대기 중인 댓글 신고 목록 조회");
        return reportCommentRepository.findAll().stream()
                .filter(report -> "PENDING".equals(report.getStatus()))
                .map(this::convertToCommentReportDTO) // 댓글 전용 변환기 사용
                .collect(Collectors.toList());
    }
    
    // --- [3. 신고 승인 처리 로직] ---

    // 게시글 신고 승인 처리
    @Transactional
    public void approveBoardReport(Long reportId) {
        BoardReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 내역을 찾을 수 없습니다."));
        report.approve(); 

        Board board = boardRepository.findById(report.getBoardId())
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        //  DTO 객체 생성 (전달할 데이터 포맷 통일)
        BoardReportMessageDTO message = new BoardReportMessageDTO(board.getBoardId());

        //  RabbitMQ 메시지 전송 (Config 상수를 사용해야 Listener가 받을 수 있습니다)
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,                  // "msa.direct.exchange"
            RabbitMQConfig.BOARD_REPORT_APPROVE_ROUTING_KEY, // "board.report.approve.key"
            message
        );
        
        log.info("-----> [BoardReportService] 관리자 승인 완료. RabbitMQ로 숨김 메시지 발행: {}", message);
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

    // --- [4. 신고 내역 삭제 로직] ---

    @Transactional
    public void deleteBoardReport(Long reportId) {
        log.info("-----> [BoardReportService] 신고 내역 삭제 시작: reportId={}", reportId);

        // 신고 내역이 존재하는지 확인 후 삭제
        if (!reportRepository.existsById(reportId)) {
            throw new RuntimeException("삭제할 신고 내역이 존재하지 않습니다.");
        }
        
        reportRepository.deleteById(reportId);
        log.info("-----> [BoardReportService] 신고 내역 삭제 완료");
    }
    
    // --- [5. DTO 변환 헬퍼 메서드] ---

    // 게시글 신고 변환
    private ReportBoardDTO convertToReportDTO(BoardReport report) {
        Board board = boardRepository.findById(report.getBoardId()).orElse(null);
        return ReportBoardDTO.builder()
                .reportId(report.getReportId())
                .boardId(report.getBoardId())
                .postTitle(board != null ? board.getTitle() : "삭제된 게시물")
                .content(board != null ? board.getContent() : "내용을 불러올 수 없습니다.")
                .memberId(report.getMemberId())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }

    // 댓글 신고 변환 (추가됨)
    private ReportBoardDTO convertToCommentReportDTO(ReportComment report) {
        Comment comment = commentRepository.findById(report.getCommentId()).orElse(null);
        return ReportBoardDTO.builder()
                .reportId(report.getReportId())
                .boardId(null) // 댓글이므로 boardId 대신 null
                .postTitle("댓글 신고 내역") // 리스트 제목에 표시될 내용
                .content(comment != null ? comment.getContent() : "삭제된 댓글입니다.")
                .memberId(report.getMemberId())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}