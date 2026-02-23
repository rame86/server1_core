package com.example.board.mapper;

import com.example.board.dto.BoardDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BoardMapper {
    // 카테고리별 목록 조회 (category가 null이면 전체 조회)
    List<BoardDTO> findAll(@Param("category") String category);
    
    BoardDTO findById(Long id);
    
    int insert(BoardDTO board);
    
    int update(BoardDTO board);
    
    int delete(Long id);
    
    // 조회수 증가
    void incrementViewCount(Long id);
}