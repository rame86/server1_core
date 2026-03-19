package com.example.board.repository;

import com.example.board.entity.LikeBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface LikeRepository extends JpaRepository<LikeBoard, Long> {
    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);
    
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByBoardIdAndMemberId(Long boardId, Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByBoardId(Long boardId);
}