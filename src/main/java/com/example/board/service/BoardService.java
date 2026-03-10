package com.example.board.service;

import com.example.board.dto.BoardCreateRequest;
import com.example.board.dto.BoardDTO;
import com.example.board.dto.BoardResponseDTO;
import com.example.board.entity.Board;
import com.example.board.entity.Comment;
import com.example.board.entity.LikeBoard;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.CommentRepository;
import com.example.board.repository.LikeRepository;
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

    private final BoardRepository boardRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    @Value("${file.upload.dir}")
    private String uploadDir;

    // 1. 목록 조회
    @Transactional(readOnly = true)
    public List<BoardDTO> getBoardList(String category) {
        String searchCategory = (category == null || category.isEmpty() || "전체".equals(category)) ? "전체" : category;
        List<Board> boards = "전체".equals(searchCategory) 
                ? boardRepository.findAllByOrderByCreatedAtDesc() 
                : boardRepository.findByCategoryOrderByCreatedAtDesc(searchCategory);
        return boards.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // 2. 상세 조회
    @Transactional
    public BoardDTO getBoardDetail(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다. ID: " + id));
        board.setViewCount(board.getViewCount() + 1);
        return convertToDTO(board);
    }

    // 3. 작성 (BoardCreateRequest 사용)
    @Transactional
    public BoardResponseDTO writeBoard(BoardCreateRequest request, MultipartFile file, Long memberId) throws IOException {
        String originalFileName = null;
        String storedFilePath = null;

        if (file != null && !file.isEmpty()) {
            File folder = new File(uploadDir);
            if (!folder.exists()) folder.mkdirs();

            originalFileName = file.getOriginalFilename();
            String storedFileName = UUID.randomUUID().toString() + "_" + originalFileName;
            storedFilePath = uploadDir + (uploadDir.endsWith("/") || uploadDir.endsWith("\\") ? "" : File.separator) + storedFileName;
            file.transferTo(new File(storedFilePath));
        }

        Board board = Board.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .memberId(memberId)
                .originalFileName(originalFileName)
                .storedFilePath(storedFilePath)
                .viewCount(0).likeCount(0).commentCount(0)
                .build();

        boardRepository.save(board);
        return BoardResponseDTO.builder().boardId(board.getBoardId()).status("SUCCESS").build();
    }

    // 4. 삭제 (반환 타입 BoardResponseDTO로 일치)
    @Transactional
    public BoardResponseDTO deleteBoard(Long id, Long memberId, String role) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 게시글이 없습니다."));

        if (!board.getMemberId().equals(memberId) && !"ADMIN".equals(role)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        if (board.getStoredFilePath() != null) {
            File file = new File(board.getStoredFilePath());
            if (file.exists()) file.delete();
        }

        boardRepository.delete(board);
        return BoardResponseDTO.builder().boardId(id).status("SUCCESS").message("삭제되었습니다.").build();
    }

    // 5. 수정 (파라미터 5개 버전으로 일치)
    @Transactional
    public BoardResponseDTO updateBoard(Long id, BoardCreateRequest request, MultipartFile file, Long memberId, String role) throws IOException {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("수정할 게시글이 없습니다."));

        if (!board.getMemberId().equals(memberId) && !"ADMIN".equals(role)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        board.setTitle(request.getTitle());
        board.setContent(request.getContent());
        board.setCategory(request.getCategory());

        if (file != null && !file.isEmpty()) {
            if (board.getStoredFilePath() != null) {
                File oldFile = new File(board.getStoredFilePath());
                if (oldFile.exists()) oldFile.delete();
            }
            String originalFileName = file.getOriginalFilename();
            String storedFileName = UUID.randomUUID().toString() + "_" + originalFileName;
            String storedFilePath = uploadDir + (uploadDir.endsWith("/") ? "" : File.separator) + storedFileName;
            file.transferTo(new File(storedFilePath));
            board.setOriginalFileName(originalFileName);
            board.setStoredFilePath(storedFilePath);
        }

        return BoardResponseDTO.builder().boardId(id).status("SUCCESS").message("수정되었습니다.").build();
    }

    // 6. 좋아요 & 7. 댓글 (이미 일치함)
    @Transactional
    public int toggleLike(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId).orElseThrow();
        if (likeRepository.existsByBoardIdAndMemberId(boardId, memberId)) {
            likeRepository.deleteByBoardIdAndMemberId(boardId, memberId);
            board.setLikeCount(Math.max(0, board.getLikeCount() - 1));
        } else {
            likeRepository.save(LikeBoard.builder().boardId(boardId).memberId(memberId).build());
            board.setLikeCount(board.getLikeCount() + 1);
        }
        return board.getLikeCount();
    }

    @Transactional
    public int addComment(Long boardId, Long memberId, String content) {
        Board board = boardRepository.findById(boardId).orElseThrow();
        commentRepository.save(Comment.builder().boardId(boardId).memberId(memberId).content(content).build());
        board.setCommentCount(board.getCommentCount() + 1);
        return board.getCommentCount();
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
                .build();
    }
}