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

    // 댓글 작성
    @Transactional
    public CommentResponseDTO createComment(CommentRequestDTO requestDTO) {
        Board board = boardRepository.findById(requestDTO.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. ID: " + requestDTO.getBoardId()));

        // 댓글 엔티티 생성
        Comment comment = Comment.builder()
                .boardId(board)
                .memberId(requestDTO.getMemberId())
                .content(requestDTO.getContent())
                .status("ACTIVE") 
                .build();

        Comment savedComment = commentRepository.save(comment);
        board.incrementCommentCount(); 
        
        log.info("새 댓글 등록 완료: 게시글ID={}, 댓글ID={}", board.getBoardId(), savedComment.getCommentId());

        CommentResponseDTO response = convertToCommentDTO(savedComment);
        return response;
    }

    // 특정 게시글의 활성화된 댓글만 조회 (최신순)
    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getCommentsByBoardId(Long boardId) {
        // 1. 게시글이 존재하는지 먼저 확인 (방어 코드)
        if (!boardRepository.existsById(boardId)) {
            throw new IllegalArgumentException("해당 게시글이 존재하지 않습니다. ID: " + boardId);
        }

        // 2. ACTIVE 상태인 댓글만 조회하여 DTO로 변환 후 리스트 반환
        return commentRepository.findByBoardId_BoardIdAndStatusOrderByCreatedAtDesc(boardId, "ACTIVE")
                .stream()
                .map(this::convertToCommentDTO)
                .toList(); // Java 16 이상이라면 .collect(Collectors.toList()) 대신 간단히 .toList() 사용 가능
    }

    // 댓글 삭제 (권한 검증 추가)
    @Transactional
    public void deleteComment(Long commentId, Long memberId, String role) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 댓글이 존재하지 않습니다. ID: " + commentId));
        
        // 본인 작성 댓글이거나 관리자인 경우만 삭제 가능
        if (!comment.getMemberId().equals(memberId) && !"ADMIN".equals(role)) {
            throw new IllegalStateException("해당 댓글을 삭제할 권한이 없습니다.");
        }

        Board board = comment.getBoardId();
        commentRepository.delete(comment);
        board.decrementCommentCount(); // 게시글의 댓글 수 감소 로직 호출
        
        log.info("댓글 삭제 완료: 댓글ID={}, 게시글ID={}", commentId, board.getBoardId());
    }

    //Entity -> DTO 변환
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