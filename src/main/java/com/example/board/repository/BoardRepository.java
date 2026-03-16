package com.example.board.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.board.entity.Board;

public interface BoardRepository extends JpaRepository<Board, Long> {
    // [전체 목록] 상태가 ACTIVE인 데이터만 최신순 조회
    List<Board> findByStatusOrderByCreatedAtDesc(String status);
    
    // [카테고리별] 카테고리 일치 + 상태가 ACTIVE인 데이터만 조회
    List<Board> findByCategoryAndStatusOrderByCreatedAtDesc(String category, String status);
    
    // [아티스트별] 카테고리 + 아티스트 일치 + 상태가 ACTIVE인 데이터만 조회
    List<Board> findByCategoryAndArtistIdAndStatusOrderByCreatedAtDesc(String category, Long artistId, String status);
}