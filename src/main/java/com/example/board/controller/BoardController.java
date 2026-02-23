package com.example.board.controller;

import com.example.board.service.BoardService;
import com.example.board.dto.BoardDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/board") // 관례상 복수형 사용 권장 (데이터 API)
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 프론트엔드(React 등) 접속 허용
public class BoardController {

    private final BoardService boardService;

    /**
     * 카테고리별 게시글 목록 조회
     * GET /api/board?category=자유게시판
     */
    @GetMapping
    public ResponseEntity<List<BoardDTO>> getList(@RequestParam(defaultValue = "전체") String category) {
        log.info("게시글 목록 조회 - 카테고리: {}", category);
        List<BoardDTO> list = boardService.getBoardList(category);
        return ResponseEntity.ok(list);
    }

    /**
     * 게시글 상세 조회 (조회수 증가 포함)
     * GET /api/board/10
     */
    @GetMapping("/{id}")
    public ResponseEntity<BoardDTO> getDetail(@PathVariable("id") Long id) {
        log.info("게시글 상세 조회 - ID: {}", id);
        return ResponseEntity.ok(boardService.getBoardDetail(id));
    }

    /**
     * 게시글 작성
     * POST /api/board
     */
    @PostMapping
    public ResponseEntity<String> write(@RequestBody BoardDTO boardDTO) {
        log.info("게시글 작성 요청 - 제목: {}", boardDTO.getTitle());
        
        try {
            boardService.writeBoard(boardDTO);
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            log.error("게시글 작성 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Fail: " + e.getMessage());
        }
    }
}