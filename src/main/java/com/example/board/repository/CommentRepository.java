package com.example.board.repository;

import com.example.board.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // board 객체 안의 boardId를 참조하도록 명시
    List<Comment> findByBoardId_BoardIdAndStatusOrderByCreatedAtDesc(Long boardId, String status);
    List<Comment> findByBoardId_BoardIdOrderByCreatedAtDesc(Long boardId);
}