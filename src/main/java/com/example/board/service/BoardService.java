package com.example.board.service;

import com.example.board.mapper.BoardMapper;
import com.example.board.dto.BoardDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardMapper boardMapper;
    
    // application.properties의 file.upload.dir 값을 주입받음
    @Value("${file.upload.dir}")
    private String uploadDir;

    //게시글 전체조회
    @Transactional(readOnly = true)
    public List<BoardDTO> getBoardList(String category) {
        String searchCategory = (category == null || category.isEmpty() || "전체".equals(category)) ? "전체" : category;
        log.info("====> [Info] 게시글 목록 조회 카테고리: {}", searchCategory);
        return boardMapper.findAll(searchCategory);
    }
    // 게시글 상세조회
    @Transactional
    public BoardDTO getBoardDetail(Long id) {
        BoardDTO board = boardMapper.findById(id);
        if (board == null) {
            log.error("====> [Error] 게시글을 찾을 수 없음 ID: {}", id);
            return null;
        }
        try {
            boardMapper.incrementViewCount(id);
            board.setViewCount(board.getViewCount() + 1);
        } catch (Exception e) {
            log.warn("====> [Warn] 조회수 증가 실패: {}", e.getMessage());
        }
        return board;
    }

    // 게시글 작성 및 파일 저장
    @Transactional
    public void writeBoard(BoardDTO boardDTO, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            // 운영체제별 경로 구분자 대응 및 폴더 생성
            File folder = new File(uploadDir);
            if (!folder.exists()) {
                folder.mkdirs();
                log.info("====> [Info] 업로드 폴더 생성 완료: {}", uploadDir);
            }

            String originalFileName = file.getOriginalFilename();
            String storedFileName = UUID.randomUUID().toString() + "_" + originalFileName;
            
            // 경로 끝에 슬래시 여부 확인 후 파일 생성
            String fullPath = uploadDir.endsWith("/") || uploadDir.endsWith("\\") 
                              ? uploadDir + storedFileName 
                              : uploadDir + File.separator + storedFileName;

            file.transferTo(new File(fullPath)); // 물리 저장

            // DB 저장을 위한 정보 세팅
            boardDTO.setOriginalFileName(originalFileName);
            boardDTO.setStoredFilePath(fullPath);
        }

        // 2. 아티스트 여부 자동 설정
        boardDTO.setArtistPost("ARTIST".equalsIgnoreCase(boardDTO.getAuthorRole()));

        boardMapper.insert(boardDTO);
        log.info("====> [Info] 게시글 등록 완료: {}", boardDTO.getTitle());
    }

    // 게시글 수정 (본인 확인 포함)
    @Transactional(rollbackFor = Exception.class)
    public void updateBoard(BoardDTO boardDTO, Long currentMemberId) {
        BoardDTO existBoard = boardMapper.findById(boardDTO.getBoardId());
        if (existBoard == null) throw new IllegalStateException("수정할 게시글이 없습니다.");

        // 본인 확인 (DTO의 memberId가 아닌 DB의 기존 memberId와 비교)
        if (!existBoard.getMemberId().equals(currentMemberId)) {
            throw new IllegalStateException("본인만 수정할 수 있습니다.");
        }

        int updateRows = boardMapper.update(boardDTO);
        if (updateRows == 0) throw new IllegalStateException("게시글 수정 실패!");
        log.info("====> [Info] 게시글 수정 완료 ID: {}", boardDTO.getBoardId());
    }

    // 게시글 삭제 (본인 확인 및 파일 삭제 포함)
    @Transactional
    public void deleteBoard(Long id, Long currentMemberId) {
        BoardDTO board = boardMapper.findById(id);
        if (board == null) throw new IllegalStateException("삭제할 게시글이 없습니다.");

        // 본인 확인
        if (!board.getMemberId().equals(currentMemberId)) {
            throw new IllegalStateException("본인만 삭제할 수 있습니다.");
        }

        // 1. DB에서 게시글 삭제 (외래키 제약조건 등이 있다면 순서 주의)
        int deletedRows = boardMapper.delete(id);
        if (deletedRows == 0) throw new IllegalStateException("게시글 삭제 실패!");

        // 2. 삭제 성공 후 물리 파일 삭제 (공유 저장소 용량 관리)
        if (board.getStoredFilePath() != null) {
            File file = new File(board.getStoredFilePath());
            if (file.exists()) {
                if (file.delete()) {
                    log.info("====> [Info] 파일 삭제 완료: {}", board.getStoredFilePath());
                } else {
                    log.warn("====> [Warn] 파일 삭제 실패: {}", board.getStoredFilePath());
                }
            }
        }
        log.info("====> [Info] 게시글 삭제 완료 ID: {}", id);
    }
}