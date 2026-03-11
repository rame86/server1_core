package com.example.board.service;

import com.example.board.dto.BoardCreateRequest;
import com.example.board.dto.BoardDTO;
import com.example.board.dto.BoardResponseDTO;
import com.example.board.entity.Board;
import com.example.board.entity.Comment;
import com.example.board.entity.LikeBoard;
import com.example.board.entity.ReportBoard;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.CommentRepository;
import com.example.board.repository.LikeRepository;
import com.example.board.repository.ReportRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Value("${file.upload.dir}")
    private String uploadDir;

    // 전체 조회
    @Transactional(readOnly = true)
    public List<BoardDTO> getBoardList(String category) {
        String searchCategory = (category == null || category.isEmpty() || "전체".equals(category)) ? "전체" : category;
        List<Board> boards = "전체".equals(searchCategory) 
                ? boardRepository.findAllByOrderByCreatedAtDesc() 
                : boardRepository.findByCategoryOrderByCreatedAtDesc(searchCategory);
        return boards.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // 상세 조회
    @Transactional
    public BoardDTO getBoardDetail(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다. ID: " + id));
        
        // 엔티티의 비즈니스 로직 호출 (조회수 증가)
        board.incrementViewCount();
        return convertToDTO(board);
    }

    // 게시글 작성
    @Transactional
    public BoardResponseDTO writeBoard(BoardCreateRequest request, MultipartFile file, Long memberId) throws IOException {
        String originalFileName = null;
        String storedFilePath = null;

        if (file != null && !file.isEmpty()) {
            originalFileName = file.getOriginalFilename();
            storedFilePath = saveFile(file); // 여기서 폴더 생성 및 저장을 처리합니다.
        }

        Board board = Board.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .memberId(memberId)
                .originalFileName(originalFileName)
                .storedFilePath(storedFilePath)
                .build();

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
                         .build());
        // 댓글 수 증가
        board.updateCommentCount(true); 
        return board.getCommentCount();
    }

    // 댓글 목록 조회
    @Transactional(readOnly = true)
    public List<Comment> getComments(Long boardId) {
       // 게시글 존재 여부 먼저 확인 (선택 사항)
    if (!boardRepository.existsById(boardId)) {
        throw new IllegalArgumentException("게시글이 존재하지 않습니다.");
    }
    return commentRepository.findByBoardIdOrderByCreatedAtDesc(boardId);
    }

    // 게시글 신고
    @Transactional
    public String reportBoard(Long boardId, Long memberId, String reason){
       log.info("-----> [신고 서비스 시작] 게시글: {}, 신고자: {}, 사유: {}", boardId, memberId, reason);

        // 1. 중복 신고 체크 (ReportRepository 사용)
        if (reportRepository.existsByBoardIdAndMemberId(boardId, memberId)) {
            log.warn("-----> [신고 실패] 이미 신고된 게시글입니다.");
            return "ALREADY_REPORTED";
        }
        // 2. 엔티티 생성 및 저장
    try {
        ReportBoard report = ReportBoard.builder()
                .boardId(boardId)
                .memberId(memberId)
                .reason(reason != null ? reason : "사유 없음") // 사유가 null일 경우 대비
                .build();
        
        reportRepository.save(report);
        log.info("-----> [신고 저장 완료] DB 확인 필요");
        return "SUCCESS";
    } catch (Exception e) {
        log.error("-----> [신고 저장 에러] 원인: {}", e.getMessage());
        throw e; // 예외를 던져야 트랜잭션이 롤백되거나 에러 로그가 남습니다.
    }
}

    // 신고리스트
    public List<ReportBoard> getReportList(){
        return reportRepository.findAllByOrderByCreatedAtDesc();
    }

    // --- 내부 헬퍼 메서드 (중복 제거 및 가독성 향상) ---

    // 파일 저장 로직 (폴더 생성 포함)
    private String saveFile(MultipartFile file) throws IOException {
        File folder = new File(uploadDir);
        if (!folder.exists()) {
            folder.mkdirs(); // 기존에 있던 폴더 생성 로직을 여기로 모았습니다.
        }

        String originalFileName = file.getOriginalFilename();
        String storedFileName = UUID.randomUUID().toString() + "_" + originalFileName;
        String storedFilePath = uploadDir + (uploadDir.endsWith(File.separator) ? "" : File.separator) + storedFileName;
        
        file.transferTo(new File(storedFilePath));
        return storedFilePath;
    }

    // 물리적 파일 삭제 로직
    private void deletePhysicalFile(String filePath) {
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private BoardDTO convertToDTO(Board board) {
        return BoardDTO.builder()
                .boardId(board.getBoardId())
                .title(board.getTitle())
                .content(board.getContent())
                .category(board.getCategory())
                .memberId(board.getMemberId())
                .viewCount(board.getViewCount())
                .likeCount(board.getLikeCount())
                .commentCount(board.getCommentCount())
                .originalFileName(board.getOriginalFileName())
                .storedFilePath(board.getStoredFilePath())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt()) 
                .build();
    }
}