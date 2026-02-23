package com.example.board.service;

import com.example.board.mapper.BoardMapper;
import com.example.board.dto.BoardDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardMapper boardMapper;

    /**
     * 게시글 목록 조회
     * @param category '전체' 또는 특정 카테고리
     */
    @Transactional(readOnly = true)
    public List<BoardDTO> getBoardList(String category) {
        // category가 null이거나 비어있을 경우를 대비한 기본값 처리
        String searchCategory = (category == null || category.isEmpty()) ? "전체" : category;
        return boardMapper.findAll(searchCategory);
    }

    /**
     * 게시글 상세 조회 및 조회수 증가
     */
    @Transactional
    public BoardDTO getBoardDetail(Long id) {
        // 1. 조회수 증가
        boardMapper.incrementViewCount(id);
        
        // 2. 게시글 조회 및 예외 처리
        BoardDTO board = boardMapper.findById(id);
        if (board == null) {
            throw new IllegalArgumentException("존재하지 않는 게시글입니다. ID: " + id);
        }
        
        return board;
    }

    /**
     * 게시글 작성
     * @param boardDTO memberId와 authorRole이 포함되어야 함
     */
    @Transactional
    public void writeBoard(BoardDTO boardDTO) {
        // 데이터 무결성 검사
        if (boardDTO.getMemberId() == null) {
            throw new IllegalStateException("작성자 정보(memberId)가 없습니다.");
        }

        // 비즈니스 로직: 아티스트 여부 자동 설정 (authorRole 기준)
        if ("ARTIST".equalsIgnoreCase(boardDTO.getAuthorRole())) {
            boardDTO.setArtistPost(true);
        } else {
            boardDTO.setArtistPost(false);
        }

        boardMapper.insert(boardDTO);
    }
}