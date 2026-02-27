package com.example.board.controller;

import com.example.common.annotation.LoginUser;
import com.example.board.service.BoardService;
import com.example.board.dto.BoardDTO;
import com.example.board.dto.RedisMemberDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // @LoginUser RedisMemberDTO user 사용해서 Redis정보 가져오기
    // 게시글 보기
    @GetMapping("/list")
    public ResponseEntity<?> getBoardList(@LoginUser RedisMemberDTO user, 
        @RequestParam(defaultValue = "전체") String category) {
        return ResponseEntity.ok(boardService.getBoardList(category));
    }

    /**
     * 게시글 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<BoardDTO> getDetail(@PathVariable("id") Long id) {
        log.info("====> [GET] 게시글 상세 조회 (ID: {})", id);
        
        try {
            BoardDTO detail = boardService.getBoardDetail(id);
            if (detail == null) {
                log.warn("====> 해당 ID({})의 게시글을 찾을 수 없습니다.", id);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            log.error("====> 상세 조회 중 에러: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 게시글 작성
     */
    @PostMapping
    public ResponseEntity<?> write(@RequestBody BoardDTO boardDTO) {
        log.info("====> [POST] 게시글 작성 요청 (제목: {})", boardDTO.getTitle());
        
        // 데이터 정합성 검사: 제목이 없으면 400 에러 반환
        if (boardDTO.getTitle() == null || boardDTO.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"제목은 필수 입력 항목입니다.\"}");
        }

        try {
            boardService.writeBoard(boardDTO);
            // 성공 시 JSON 객체로 메시지 전달 (프론트엔드 처리 용이성)
            return ResponseEntity.status(201).body("{\"message\": \"Success\"}");
        } catch (Exception e) {
            log.error("====> 게시글 작성 중 에러: ", e);
            return ResponseEntity.internalServerError().body("{\"error\": \"Server Error\"}");
        }
    }
}