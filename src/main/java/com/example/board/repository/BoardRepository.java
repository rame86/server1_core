package com.example.board.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.board.entity.Board;

public interface BoardRepository extends JpaRepository<Board, Long> {
    // 숨김 처리되지 않은 전체 목록 조회
    List<Board> findByHiddenFalseOrderByCreatedAtDesc();
    
    // [수정] 카테고리별 숨김 처리되지 않은 목록 조회 (IsHidden -> Hidden)
    List<Board> findByCategoryAndHiddenFalseOrderByCreatedAtDesc(String category);
    
    // 아티스트별 조회 (필요 시 여기에도 HiddenFalse 조건을 추가하는 것이 좋습니다)
    List<Board> findByCategoryAndArtistIdOrderByCreatedAtDesc(String category, Long artistId);
}