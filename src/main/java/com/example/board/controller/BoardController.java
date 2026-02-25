package com.example.board.controller;

import com.example.board.service.BoardService;
import com.example.board.dto.BoardDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;

/**
 * 게시판 기능을 담당하는 REST 컨트롤러
 * 최신 트렌드인 생성자 주입(Lombok)과 명확한 JSON 응답 구조를 적용했습니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class BoardController {

    private final BoardService boardService;

    /**
     * 카테고리별 게시글 목록 조회
     * GET http://localhost:8080/api/board/posts
     * 만약 404가 뜨면 로그를 확인하여 http://localhost:8080/api/core/api/board/posts 로 시도하세요.
     */
    @GetMapping("/posts")
    public ResponseEntity<List<BoardDTO>> getList(@RequestParam(name = "category", defaultValue = "전체") String category) {
        log.info("====> [GET] 게시글 목록 조회 시작 (카테고리: {})", category);
        
        try {
            List<BoardDTO> list = boardService.getBoardList(category);
            
            // 데이터가 null이거나 비어있어도 브라우저에서 []가 보이도록 처리하여 
            // '404'나 'Whitelabel Error' 페이지가 뜨는 것을 방지합니다.
            if (list == null) {
                list = new ArrayList<>();
            }
            
            log.info("====> 조회 결과: {} 건의 데이터 반환", list.size());
            return ResponseEntity.ok(list); 
        } catch (Exception e) {
            log.error("====> [에러] 데이터 조회 중 예외 발생: ", e);
            // 에러 시에도 빈 배열을 반환하여 프론트엔드 중단 방지
            return ResponseEntity.internalServerError().body(new ArrayList<>());
        }
    }

    /**
     * 게시글 상세 조회
     * GET http://localhost:8080/api/board/{id}
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
     * POST http://localhost:8080/api/board
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