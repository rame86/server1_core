package com.example.board.controller;

import com.example.common.annotation.LoginUser;
import com.example.member.dto.RedisMemberDTO;
import com.example.board.service.BoardService;
import com.example.board.dto.BoardDTO;

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
        @RequestParam(name = "category", defaultValue = "전체") String category,
        @RequestParam(name = "artistId", required = false) Long artistId) {
    	log.info("-----> [BOARD LIST] 요청 카테고리: {}", category);
        return ResponseEntity.ok(boardService.getBoardList(category, artistId));
    }

    /**
     * 게시글 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable("id") Long id) {
        log.info("====> [GET] 게시글 상세 조회 (ID: {})", id);
        
        try {
            BoardDTO detail = boardService.getBoardDetail(id);
            if (detail == null) {
                return ResponseEntity.status(404).body("{\"error\": \"게시글을 찾을 수 없습니다.\"}");
            }
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            // 서버 콘솔에 에러의 구체적인 원인을 찍습니다.
            log.error("====> 상세 조회 중 에러 발생: {}", e.getMessage());
            e.printStackTrace(); 
            
            // 프론트엔드에서 어떤 에러인지 알 수 있도록 메시지를 담아 보냅니다.
            return ResponseEntity.status(500).body("{\"error\": \"서버 내부 에러: " + e.getMessage() + "\"}");
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