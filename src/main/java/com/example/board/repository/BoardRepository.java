package com.example.board.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.board.entity.Board;

public interface BoardRepository extends JpaRepository<Board, Long> {
    // [전체 목록] 숨김 처리되지 않은 데이터만 최신순 조회
    List<Board> findByHiddenFalseOrderByCreatedAtDesc();
    
    // [카테고리별] 카테고리 일치 + 숨김 처리 제외
    List<Board> findByCategoryAndHiddenFalseOrderByCreatedAtDesc(String category);
    
    // [아티스트별] 카테고리 + 아티스트 일치 + 숨김 처리 제외
    List<Board> findByCategoryAndArtistIdAndHiddenFalseOrderByCreatedAtDesc(String category, Long artistId);
}