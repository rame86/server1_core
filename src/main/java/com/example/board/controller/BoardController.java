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
@RequestMapping("/api/board") // 프론트엔드 API 호출 경로와 일치시킴
@RequiredArgsConstructor // final이 붙은 필드에 대해 생성자 주입 자동 생성
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class BoardController {

    // @Autowired 제거: @RequiredArgsConstructor와 final을 통한 생성자 주입이 최신 트렌드 및 권장 사항입니다.
    private final BoardService boardService;

    /**
     * 카테고리별 게시글 목록 조회
     * GET /api/board/posts?category=자유게시판
     */
    @GetMapping("/posts")
    public ResponseEntity<List<BoardDTO>> getList(@RequestParam(name = "category", defaultValue = "전체") String category) {
        log.info("게시글 목록 조회 요청 - 카테고리: {}", category);
        List<BoardDTO> list = boardService.getBoardList(category);
        
        // 데이터가 없을 때 빈 리스트를 반환하거나 204 No Content를 반환할 수 있습니다.
        if (list.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(list);
    }

    /**
     * 게시글 상세 조회
     * GET /api/board/10
     */
    @GetMapping("/{id}")
    public ResponseEntity<BoardDTO> getDetail(@PathVariable("id") Long id) {
        log.info("게시글 상세 조회 요청 - ID: {}", id);
        BoardDTO detail = boardService.getBoardDetail(id);
        
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }

    /**
     * 게시글 작성
     * POST /api/board
     */
    @PostMapping
    public ResponseEntity<?> write(@RequestBody BoardDTO boardDTO) {
        log.info("게시글 작성 요청 - 제목: {}", boardDTO.getTitle());
        
        // 보안/유효성 검사 (간이 예시)
        if (boardDTO.getTitle() == null || boardDTO.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("제목은 필수 입력 항목입니다.");
        }

        try {
            boardService.writeBoard(boardDTO);
            // 성공 시 단순 문자열보다 JSON 형태의 응답이나 상태 코드를 권장합니다.
            return ResponseEntity.status(201).body("게시글이 성공적으로 등록되었습니다.");
        } catch (Exception e) {
            log.error("게시글 작성 중 예외 발생: ", e);
            return ResponseEntity.internalServerError().body("서버 오류로 인해 등록에 실패했습니다.");
        }
    }
}