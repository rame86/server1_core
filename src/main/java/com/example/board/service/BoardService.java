package com.example.board.service;

import com.example.board.dto.BoardCreateRequest;
import com.example.board.dto.BoardDTO;
import com.example.board.dto.BoardResponseDTO;
import com.example.board.dto.CommentResponseDTO;
import com.example.board.entity.Board;
import com.example.board.entity.Comment;
import com.example.board.entity.LikeBoard;
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
    private final BoardRepository boardRepository; 
    private final ReportRepository reportRepository; 
    private final ReportCommentRepository reportCommentRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Transactional
    public void hideBoardByMessage(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다. ID: " + boardId));
        board.hideBoard();
        log.info("메시지 수신: 게시글 ID {} 가 숨김 처리되었습니다.", boardId);
    }
   
    @Transactional(readOnly = true)
    public List<BoardDTO> getBoardList(String category) {
        String searchCategory = (category == null || category.isEmpty() || "전체".equals(category)) ? "전체" : category;
        List<Board> boards = "전체".equals(searchCategory) ?
            boardRepository.findByStatusOrderByCreatedAtDesc("ACTIVE") : 
            boardRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, "ACTIVE");
        return boards.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public BoardDTO getBoardDetail(Long id) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        board.incrementViewCount();
        return convertToDTO(board);
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
        Board board = boardRepository.findById(boardId).orElseThrow();
        if (likeRepository.existsByBoardIdAndMemberId(boardId, memberId)) {
            likeRepository.deleteByBoardIdAndMemberId(boardId, memberId);
            board.updateLikeCount(false); 
        } else {
            likeRepository.save(LikeBoard.builder().boardId(boardId).memberId(memberId).build());
            board.updateLikeCount(true); 
        }
        return board.getLikeCount();
    }

    @Transactional
    public int addComment(Long boardId, Long memberId, String content) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(()-> new IllegalArgumentException("게시글이 없습니다."));
        
        commentRepository.save(Comment.builder()
                                 .boardId(board)
                                 .memberId(memberId)
                                 .content(content)
                                 .status("ACTIVE")
                                 .build());
        
        board.incrementCommentCount(); 
        return board.getCommentCount();
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getComments(Long boardId) {
        if (!boardRepository.existsById(boardId)) throw new IllegalArgumentException("게시글 없음");
        // [수정] Comment 엔티티 내 필드명이 board인 경우 findByBoard_BoardId를 사용합니다.
       return commentRepository.findByBoardId_BoardIdAndStatusOrderByCreatedAtDesc(boardId, "ACTIVE")
                .stream().map(this::convertToCommentDTO).toList();
    }

    private CommentResponseDTO convertToCommentDTO(Comment comment) {
        return CommentResponseDTO.builder()
                .commentId(comment.getCommentId())
                .boardId(comment.getBoardId() != null ? comment.getBoardId().getBoardId() : null)
                .memberId(comment.getMemberId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .status(comment.getStatus())
                .authorName("User_" + comment.getMemberId())
                .build();
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
        return BoardDTO.builder()
                .boardId(board.getBoardId()).title(board.getTitle()).content(board.getContent())
                .category(board.getCategory()).memberId(board.getMemberId()).viewCount(board.getViewCount())
                .status(board.getStatus()).likeCount(board.getLikeCount()).commentCount(board.getCommentCount())
                .originalFileName(board.getOriginalFileName()).storedFilePath(fileUrl)
                .createdAt(board.getCreatedAt()).updatedAt(board.getUpdatedAt()).authorName(board.getMemberId() != null ? "User_" + board.getMemberId() : "익명")
                .artistPost(board.isArtistPost()).build();
    }
}