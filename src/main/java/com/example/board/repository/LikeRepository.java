package com.example.board.repository;

import com.example.board.entity.LikeBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface LikeRepository extends JpaRepository<LikeBoard, Long> {
    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);
    @Modifying
    void deleteByBoardIdAndMemberId(Long boardId, Long memberId);
}