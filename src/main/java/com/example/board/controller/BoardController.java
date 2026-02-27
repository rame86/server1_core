package com.example.board.controller;

import com.example.board.service.BoardService;
import com.example.board.dto.BoardDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;

@Slf4j
@RestController
@RequestMapping("/board") // 포스트맨 'path: /board/list' 결과에 맞춤
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // MSA 환경에 맞춰 모든 오리진 허용
public class BoardController {

    private final BoardService boardService;

    /**
     * 게시글 목록 조회
     * GET /board/list?category=전체
     */
    @GetMapping("/list")
    public ResponseEntity<List<BoardDTO>> getList(@RequestParam(name = "category", defaultValue = "전체") String category) {
        log.info("====> [MSA Board] 목록 조회 호출: {}", category);
        try {
            List<BoardDTO> list = boardService.getBoardList(category);
            return ResponseEntity.ok(list != null ? list : new ArrayList<>());
        } catch (Exception e) {
            log.error("====> [Error] 목록 조회 실패: ", e);
            return ResponseEntity.internalServerError().body(new ArrayList<>());
        }
    }

    /**
     * 게시글 상세 조회
     * GET /board/detail/{id}
     */
    @GetMapping("{id}")
    public ResponseEntity<BoardDTO> getDetail(@PathVariable("id") Long id) {
        log.info("====> [MSA Board] 상세 조회 호출 ID: {}", id);
        try {
            BoardDTO detail = boardService.getBoardDetail(id);
            if (detail == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 게시글 작성 (포스트맨 테스트용)
     * POST /board/write
     */
    @PostMapping("/write")
    public ResponseEntity<?> write(@RequestBody BoardDTO boardDTO) {
        log.info("====> [MSA Board] 게시글 작성 호출: {}", boardDTO.getTitle());
        try {
            boardService.writeBoard(boardDTO);
            return ResponseEntity.status(201).body("{\"message\": \"Success\"}");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}