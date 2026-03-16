package com.example.board.service;

import com.example.board.dto.BoardCreateRequest;
import com.example.board.dto.BoardDTO;
import com.example.board.dto.BoardResponseDTO;
import com.example.board.dto.CommentResponseDTO;
import com.example.board.dto.ReportBoardDTO; // 추가
import com.example.board.entity.Board;
import com.example.board.entity.Comment;
import com.example.board.entity.LikeBoard;
import com.example.board.entity.ReportComment;
import com.example.board.entity.BoardReport;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.CommentRepository;
import com.example.board.repository.LikeRepository;
import com.example.board.repository.ReportCommentRepository;
import com.example.board.repository.ReportRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;     // 조회용 (기존 필드)
    private final ReportRepository reportRepository;   // 저장용 (새로 추가)
    private final ReportCommentRepository reportCommentRepository; // 댓글 신고용 레포지토리 추가
    private final RabbitTemplate rabbitTemplate; // RabbitMQ 템플릿 주입

    @Value("${file.upload.dir}")
    private String uploadDir;

    // RabbitMQ 리스너로부터 호출될 실제 승인 처리 로직
    @Transactional
    public void hideBoardByMessage(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다. ID: " + boardId));
        board.hideBoard(); // hidden = true
        log.info("메시지 수신: 게시글 ID {} 가 숨김 처리되었습니다.", boardId);
    }
   
    // 전체 조회
    @Transactional(readOnly = true)
    public List<BoardDTO> getBoardList(String category) {
        String searchCategory = (category == null || category.isEmpty() || "전체".equals(category)) ? "전체" : category;
        List<Board> boards = "전체".equals(searchCategory) ?
            boardRepository.findByStatusOrderByCreatedAtDesc("ACTIVE") : 
            boardRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, "ACTIVE");
        return boards.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // 상세 조회
    @Transactional
    public BoardDTO getBoardDetail(Long id) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        board.incrementViewCount();
        return convertToDTO(board);
    }

    // 게시글 작성
    @Transactional
    public BoardResponseDTO writeBoard(BoardCreateRequest request, MultipartFile file, Long memberId) throws IOException {
        String originalFileName = (file != null && !file.isEmpty()) ? file.getOriginalFilename() : null;
        String storedFilePath = (file != null && !file.isEmpty()) ? saveFile(file) : null;

        Board board = Board.builder()
                .title(request.getTitle()).content(request.getContent())
                .category(request.getCategory()).memberId(memberId)
                .originalFileName(originalFileName).storedFilePath(storedFilePath).build();

        boardRepository.save(board);
        return BoardResponseDTO.builder().boardId(board.getBoardId()).status("SUCCESS").build();
    }

    // 게시글 삭제
    @Transactional
    public BoardResponseDTO deleteBoard(Long id, Long memberId, String role) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 게시글이 없습니다."));

        // 401/403 방지를 위한 권한 체크 로그
        log.info("Delete Attempt - Board MemberId: {}, Request MemberId: {}, Role: {}", board.getMemberId(), memberId, role);
        
        if (!board.getMemberId().equals(memberId) && !"ADMIN".equals(role)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        deletePhysicalFile(board.getStoredFilePath());
        boardRepository.delete(board);
        return BoardResponseDTO.builder().boardId(id).status("SUCCESS").message("삭제되었습니다.").build();
    }

    // 게시글 수정
    @Transactional
    public BoardResponseDTO updateBoard(Long id, BoardCreateRequest request, MultipartFile file, Long memberId, String role) throws IOException {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("수정할 게시글이 없습니다."));

        if (!board.getMemberId().equals(memberId) && !"ADMIN".equals(role)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }
        // 텍스트 정보 업데이트
        board.update(request.getTitle(), request.getContent(), request.getCategory());
        // 파일 처리 로직
        if (file != null && !file.isEmpty()) {
            deletePhysicalFile(board.getStoredFilePath()); // 기존 파일 물리적 삭제
            String originalFileName = file.getOriginalFilename();
            String storedFilePath = saveFile(file); // 새 파일 저장 및 폴더 생성 체크
            board.updateFile(originalFileName, storedFilePath);
        }else if (request.getFileDeleted() != null && request.getFileDeleted()) {
            // [추가된 로직] 사용자가 파일을 삭제하고 싶다고 명시한 경우 (DTO에 필드 추가 필요)
            deletePhysicalFile(board.getStoredFilePath());
            board.updateFile(null, null); // DB 정보를 NULL로 변경
        }

        return BoardResponseDTO.builder().boardId(id).status("SUCCESS").message("수정되었습니다.").build();
    }

    // 좋아요 
    @Transactional
    public int toggleLike(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId).orElseThrow();
        if (likeRepository.existsByBoardIdAndMemberId(boardId, memberId)) {
            likeRepository.deleteByBoardIdAndMemberId(boardId, memberId);
            // 직접 setLikeCount 하지 않고 엔티티 메서드 호출
            board.updateLikeCount(false); 
        } else {
            likeRepository.save(LikeBoard.builder().boardId(boardId).memberId(memberId).build());
            // 직접 setLikeCount 하지 않고 엔티티 메서드 호출
            board.updateLikeCount(true); 
        }
        return board.getLikeCount();
    }

    // 게시글 댓글 작성 
    @Transactional
    public int addComment(Long boardId, Long memberId, String content) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(()-> new IllegalArgumentException("게시글이 없습니다."));
        commentRepository.save(Comment.builder()
                         .boardId(boardId)
                         .memberId(memberId)
                         .content(content)
                         .status("ACTIVE") // 초기 상태 설정
                         .build());
        // 댓글 수 증가
        board.updateCommentCount(true); 
        return board.getCommentCount();
    }

    // 댓글 목록 조회
    @Transactional(readOnly = true)
   public List<CommentResponseDTO> getComments(Long boardId) {
        if (!boardRepository.existsById(boardId)) throw new IllegalArgumentException("게시글 없음");
        return commentRepository.findByBoardIdOrderByCreatedAtDesc(boardId)
                .stream().map(CommentResponseDTO::from).toList();
    }
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

    // 신고 목록 조회들
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
    public void approveReport(Long reportId) {
        // 1. 신고 내역 조회
        BoardReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 내역을 찾을 수 없습니다."));
        report.approve(); // 2. 신고 상태 변경
        // 3. 대상 게시글 상태 변경 (숨김 처리)
        Board board = boardRepository.findById(report.getBoardId())
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 3. RabbitMQ로 메시지 전송 (이 부분이 "전화를 거는" 역할)
        // exchange 이름과 routingKey는 설정하신 값에 맞춰주세요.
        rabbitTemplate.convertAndSend("board.exchange", "board.hide", board.getBoardId());
        
        log.info("관리자 승인: 신고 ID {} 승인. RabbitMQ로 게시글 ID {} 숨김 요청 전송", reportId, board.getBoardId());
    }
    // 댓글 신고 승인 처리
    public void approveCommentReport(Long reportId) {
        ReportComment report = reportCommentRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("댓글 신고 내역을 찾을 수 없습니다."));
        
        report.approve();

        Comment comment = commentRepository.findById(report.getCommentId())
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        
        // Comment 엔티티에도 status 필드나 hidden 필드가 있다면 그에 맞춰 수정
        comment.hideComment();
        
        log.info("관리자 승인: 댓글 신고 ID {} 승인 완료", reportId);
    }

// --- 내부 헬퍼 메서드 ---

    private String saveFile(MultipartFile file) throws IOException {
        File folder = new File(uploadDir);
        if (!folder.exists()) folder.mkdirs();

        String storedFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String storedFilePath = uploadDir + File.separator + storedFileName;
        file.transferTo(new File(storedFilePath));
        return storedFilePath;
    }

    private void deletePhysicalFile(String filePath) {
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) file.delete();
        }
    }

    private BoardDTO convertToDTO(Board board) {
        return BoardDTO.builder()
                .boardId(board.getBoardId()).title(board.getTitle())
                .content(board.getContent()).category(board.getCategory())
                .memberId(board.getMemberId()).viewCount(board.getViewCount()).status(board.getStatus())
                .likeCount(board.getLikeCount()).commentCount(board.getCommentCount())
                .originalFileName(board.getOriginalFileName()).storedFilePath(board.getStoredFilePath())
                .createdAt(board.getCreatedAt()).updatedAt(board.getUpdatedAt()).build();
    }

    private ReportBoardDTO convertToReportDTO(BoardReport report) {
        return ReportBoardDTO.builder()
                .reportId(report.getReportId()).boardId(report.getBoardId())
                .memberId(report.getMemberId()).reason(report.getReason())
                .status(report.getStatus()).createdAt(report.getCreatedAt()).build();
    }
}