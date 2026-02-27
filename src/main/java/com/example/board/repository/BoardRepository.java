package com.example.board.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.board.entity.Board;

public interface BoardRepository extends JpaRepository<Board, Long> {
    // 카테고리가 '전체'일 때 최신순으로 정렬
    List<Board> findAllByOrderByCreatedAtDesc();

    // 2. 특정 카테고리를 선택했을 때용 (카테고리 조건 + 최신순 정렬)
    List<Board> findByCategoryOrderByCreatedAtDesc(String category);
}
