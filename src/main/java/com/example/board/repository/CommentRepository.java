package com.example.board.repository;

import com.example.board.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 특정 게시글의 댓글 중 'ACTIVE' 상태인 것만 최신순으로 조회
    List<Comment> findByBoardIdAndStatusOrderByCreatedAtDesc(Long boardId, String status);
    List<Comment> findByBoardIdOrderByCreatedAtDesc(Long boardId);
}