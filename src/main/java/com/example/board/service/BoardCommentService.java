package com.example.board.service;

import com.example.board.dto.CommentRequestDTO;
import com.example.board.dto.CommentResponseDTO;
import com.example.board.entity.Board;
import com.example.board.entity.Comment;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardCommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;

    /**
     * 댓글 작성
     */
    @Transactional
    public CommentResponseDTO createComment(CommentRequestDTO requestDTO) {
        Board board = boardRepository.findById(requestDTO.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. ID: " + requestDTO.getBoardId()));

        // [수정] .boardId(boardId) -> .board(board) 객체 직접 전달
        Comment comment = Comment.builder()
                .boardId(board)
                .memberId(requestDTO.getMemberId())
                .content(requestDTO.getContent())
                .status("ACTIVE") 
                .build();

        Comment savedComment = commentRepository.save(comment);
        board.incrementCommentCount(); 
           log.info("새 댓글 등록 완료: 게시글ID={}, 댓글ID={}", board.getBoardId(), savedComment.getCommentId());

           return convertToCommentDTO(savedComment);
    }

    // 특정 게시글의 활성화된 댓글만 조회 (최신순)
    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getCommentsByBoardId(Long boardId) {
        // [수정] ACTIVE 상태인 댓글만 필터링 조회
        return commentRepository.findByBoardId_BoardIdAndStatusOrderByCreatedAtDesc(boardId, "ACTIVE").stream()
                .map(this::convertToCommentDTO)
                .collect(Collectors.toList());
    }

    //댓글 삭제
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 댓글이 존재하지 않습니다. ID: " + commentId));
        
        Board board = comment.getBoardId();
        commentRepository.delete(comment);
        board.decrementCommentCount();
    }

    private CommentResponseDTO convertToCommentDTO(Comment comment) {
        return CommentResponseDTO.builder()
                .commentId(comment.getCommentId())
                .boardId(comment.getBoardId().getBoardId()) // boardId 필드(Board 객체)에서 getBoardId() 호출
                .memberId(comment.getMemberId())
                .content(comment.getContent())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt())
                .authorName("User_" + comment.getMemberId())
                .build();
    }
}