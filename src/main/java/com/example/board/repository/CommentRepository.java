package com.example.board.repository;

import com.example.board.entity.Comment;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // board 객체 안의 boardId를 참조하도록 명시
    List<Comment> findByBoardId_BoardIdAndStatusOrderByCreatedAtDesc(Long boardId, String status);
    List<Comment> findByBoardId_BoardIdOrderByCreatedAtDesc(Long boardId);

    @Modifying
    @Transactional
    void deleteByBoardId_BoardId(Long boardId); // 게시글 ID로 모든 댓글 삭제
}