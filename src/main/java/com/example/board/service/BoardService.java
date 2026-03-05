package com.example.board.service;

import com.example.board.mapper.BoardMapper;
import com.example.board.repository.BoardRepository;
import com.example.board.dto.BoardDTO;
import com.example.board.entity.Board;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardMapper boardMapper;
    private final BoardRepository boardRepository;

    // 게시글 목록 조회 (카테고리 필터링 포함)
    @Transactional(readOnly = true)
    public List<BoardDTO> getBoardList(String category) {
        // 카테고리가 없거나 "전체"인 경우 "전체"로 설정하여 매퍼에 전달
        String searchCategory = (category == null || category.isEmpty() || "전체".equals(category)) ? "전체" : category;
        log.info("====> [Info] 게시글 목록 조회 카테고리: {}", searchCategory);
        return boardMapper.findAll(searchCategory);
    }

    // 게시글 상세 조회 및 조회수 증가
    @Transactional
    public BoardDTO getBoardDetail(Long id) {
        // 1. 게시글 존재 여부 확인 및 조회
        BoardDTO board = boardMapper.findById(id);
        
        if (board == null) {
            log.error("====> [Error] 게시글을 찾을 수 없음 ID: {}", id);
            return null; // Controller에서 404 처리를 위해 null 반환
        }
        
        // 2. 조회수 증가 (조회 성공 시에만)
        try {
            boardMapper.incrementViewCount(id);
            board.setViewCount(board.getViewCount() + 1); // 반영된 결과 DTO에 세팅
        } catch (Exception e) {
            log.warn("====> [Warn] 조회수 증가 실패: {}", e.getMessage());
        }
        
        return board;
    }

    // 게시글 작성 @param boardDTO memberId와 authorRole이 포함되어야 함
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

    // 게시글 수정
    @Transactional
    public void updateBoard(BoardDTO boardDTO){
        if (boardDTO.getBoardId() == null) {
            throw new IllegalArgumentException("수정할 게시글 번호가 없습니다.");
        }
        log.info("===>게시글 수정시도 ID: {}, 작성자: {}", boardDTO.getBoardId(),boardDTO.getMemberId());
    
        int updateRows = boardMapper.update(boardDTO);

        if (updateRows == 0) {
            throw new IllegalStateException("게시글 수정 실패!");
        }
    }

    // 게시글 삭제
    @Transactional
    public void deleteBoard(Long id){
        log.info("===> 게시글 삭제시도 id:{}", id);

        int deletedRows = boardMapper.delete(id);

        if (deletedRows == 0){
            throw new IllegalStateException("게시글 삭제 실패!");
        }
    }
}