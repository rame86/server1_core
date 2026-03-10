package com.example.board.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.board.entity.Board;

public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findAllByOrderByCreatedAtDesc();
    List<Board> findByCategoryOrderByCreatedAtDesc(String category);
    List<Board> findByCategoryAndArtistIdOrderByCreatedAtDesc(String category, Long artistId);
}