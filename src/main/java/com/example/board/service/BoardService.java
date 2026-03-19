package com.example.board.service;

import com.example.board.dto.BoardCreateRequest;
import com.example.board.dto.BoardDTO;
import com.example.board.dto.BoardResponseDTO;
import com.example.board.entity.Board;
import com.example.board.entity.LikeBoard;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.LikeRepository;
import com.example.board.repository.CommentRepository;
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
    private final BoardRepository boardRepository; 
    private final CommentRepository commentRepository;
       
    @Value("${file.upload.dir}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public List<BoardDTO> getBoardList(String category) {
        String searchCategory = (category == null || category.isEmpty() || "전체".equals(category)) ? "전체" : category;
        List<Board> boards = "전체".equals(searchCategory) ?
            boardRepository.findByStatusOrderByCreatedAtDesc("ACTIVE") : 
            boardRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, "ACTIVE");
        return boards.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public void hideBoardByMessage(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다. ID: " + boardId));
        board.hideBoard();
        log.info("메시지 수신: 게시글 ID {} 가 숨김 처리되었습니다.", boardId);
    }


    @Transactional
    public BoardDTO getBoardDetail(Long id) {
    // 1. DB에서 게시글 조회
    Board board = boardRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

    board.incrementViewCount();
    BoardDTO dto = convertToDTO(board);
    
    // 4. 아티스트 여부 체크 (기존 DTO에 있는 메서드)
    dto.checkArtistStatus();

    return dto;
}

    @Transactional
    public BoardResponseDTO writeBoard(BoardCreateRequest request, MultipartFile file, Long memberId) throws IOException {
        String originalFileName = null;
        String storedFileName = null;

        if (file != null && !file.isEmpty()) {
            originalFileName = file.getOriginalFilename();
            String fullPath = saveFile(file);
            storedFileName = new File(fullPath).getName();
        }

        Board board = Board.builder()
                .title(request.getTitle()).content(request.getContent())
                .category(request.getCategory()).memberId(memberId)
                .originalFileName(originalFileName).storedFilePath(storedFileName).status("ACTIVE").build();
        boardRepository.save(board);
        return BoardResponseDTO.builder().boardId(board.getBoardId()).status("SUCCESS").build();
    }

    @Transactional
    public BoardResponseDTO deleteBoard(Long id, Long memberId, String role) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 게시글이 없습니다."));

        if (!board.getMemberId().equals(memberId) && !"ADMIN".equals(role)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        
        // 연관 데이터 삭제 (순서 중요)
        commentRepository.deleteByBoardId_BoardId(id); // 댓글 먼저 삭제
        likeRepository.deleteByBoardId(id);    // 좋아요 삭제
        // 파일 삭제
        if (board.getStoredFilePath() != null) {
            deletePhysicalFile(uploadDir + File.separator + board.getStoredFilePath());
        }
      
        boardRepository.delete(board);
        return BoardResponseDTO.builder().boardId(id).status("SUCCESS").message("삭제되었습니다.").build();
    }

    @Transactional
    public BoardResponseDTO updateBoard(Long id, BoardCreateRequest request, MultipartFile file, Long memberId, String role) throws IOException {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("수정할 게시글이 없습니다."));

        if (!board.getMemberId().equals(memberId) && !"ADMIN".equals(role)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }
        
        board.update(request.getTitle(), request.getContent(), request.getCategory());
       
        if (file != null && !file.isEmpty()) {
            if (board.getStoredFilePath() != null) {
                deletePhysicalFile(uploadDir + File.separator + board.getStoredFilePath());
            }
            String originalFileName = file.getOriginalFilename();
            String fullPath = saveFile(file);
            board.updateFile(originalFileName, new File(fullPath).getName());
        } else if (Boolean.TRUE.equals(request.getFileDeleted())) {
            if (board.getStoredFilePath() != null) {
                deletePhysicalFile(uploadDir + File.separator + board.getStoredFilePath());
            }
            board.updateFile(null, null);
        }

        return BoardResponseDTO.builder().boardId(id).status("SUCCESS").message("수정되었습니다.").build();
    }

    @Transactional
    public int toggleLike(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        
        // 1. 이미 좋아요를 눌렀는지 확인
        boolean exists = likeRepository.existsByBoardIdAndMemberId(boardId, memberId);

        if (exists) {
            // 2. 이미 있다면 삭제 (좋아요 취소)
            likeRepository.deleteByBoardIdAndMemberId(boardId, memberId);
            board.updateLikeCount(false);
            log.info("좋아요 취소: 게시글={}, 회원={}", boardId, memberId);
        } else {
            // 3. 없다면 추가 (좋아요)
            // 중복 클릭 방지를 위해 한 번 더 체크하거나 try-catch로 DB 예외 처리 가능
            try {
                likeRepository.save(LikeBoard.builder()
                        .boardId(boardId)
                        .memberId(memberId)
                        .build());
                board.updateLikeCount(true);
                log.info("좋아요 추가: 게시글={}, 회원={}", boardId, memberId);
            } catch (Exception e) {
                log.warn("이미 좋아요 처리가 진행 중입니다.");
            }
        }
        // 변경된 count 반영
        boardRepository.save(board); 
        return board.getLikeCount();
    }

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
        String fileUrl = board.getStoredFilePath() != null 
                ? "/msa/core/board/files/" + board.getStoredFilePath() 
                : null;
        String uiAuthorName = "User_" + (board.getMemberId() != null ? board.getMemberId() : "익명");

        return BoardDTO.builder()
                .boardId(board.getBoardId()).title(board.getTitle()).content(board.getContent())
                .category(board.getCategory()).memberId(board.getMemberId()).viewCount(board.getViewCount())
                .status(board.getStatus()).likeCount(board.getLikeCount()).commentCount(board.getCommentCount())
                .originalFileName(board.getOriginalFileName()).storedFilePath(fileUrl).artistPost(board.isArtistPost())
                .createdAt(board.getCreatedAt()).updatedAt(board.getUpdatedAt()).authorName(uiAuthorName).build();
    }
}