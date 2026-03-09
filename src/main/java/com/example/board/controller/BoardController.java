package com.example.board.controller;

import com.example.board.service.BoardService;
import com.example.board.dto.BoardDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Slf4j
@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BoardController {

    private final BoardService boardService;
    private final StringRedisTemplate redisTemplate;

    /**
     * Redis에서 토큰을 통해 memberId를 추출하는 공통 메서드
     */
    private Long getMemberIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) return null;
        String redisKey = "session:" + token.substring(7);
        String memberIdStr = redisTemplate.opsForValue().get(redisKey);
        return (memberIdStr != null) ? Long.parseLong(memberIdStr) : null;
    }

    /**
     * 게시글 작성: 파일 업로드 및 Redis 인증 포함
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> write(
            @RequestHeader("Authorization") String token,
            @RequestPart("board") BoardDTO boardDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        log.info("====> [MSA Board] 게시글 작성 호출: {}", boardDTO.getTitle());
        try {
            Long memberId = getMemberIdFromToken(token);
            if (memberId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "인증토큰이 유효하지 않습니다."));
            }

            boardDTO.setMemberId(memberId);
            boardService.writeBoard(boardDTO, file);

            return ResponseEntity.status(201).body(Map.of("message", "Success"));
        } catch (Exception e) {
            log.error("====> [Error] 게시글 작성 실패: ", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "게시글 작성 중 오류가 발생했습니다."));
        }
    }

    /**
     * 게시글 수정: 본인 확인(memberId) 필수
     */
    @PutMapping("/update")
    public ResponseEntity<?> update(
            @RequestHeader("Authorization") String token,
            @RequestBody BoardDTO boardDTO) {
        
        log.info("====> [MSA Board] 게시글 수정 호출 id: {}", boardDTO.getBoardId());
        try {
            Long memberId = getMemberIdFromToken(token);
            if (memberId == null) return ResponseEntity.status(401).body(Map.of("message", "Invalid Token"));

            // 서비스 호출 시 수정 권한 확인을 위해 memberId 전달
            boardService.updateBoard(boardDTO, memberId);
            return ResponseEntity.ok(Map.of("message", "Update Success"));
        } catch (IllegalStateException e) {
            log.error("====> [Error] 수정 권한 없음: {}", e.getMessage());
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("====> [Error] 수정 실패: ", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Update Failed"));
        }
    }

    /**
     * 게시글 삭제: 본인 확인(memberId) 필수
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @RequestHeader("Authorization") String token, 
            @PathVariable("id") Long id) {
        
        log.info("====> [MSA Board] 게시글 삭제 호출 id: {}", id);
        try {
            Long memberId = getMemberIdFromToken(token);
            if (memberId == null) return ResponseEntity.status(401).body(Map.of("message", "Invalid Token"));

            boardService.deleteBoard(id, memberId);
            return ResponseEntity.ok(Map.of("message", "Delete Success"));
        } catch (IllegalStateException e) {
            log.error("====> [Error] 삭제 권한 없음: {}", e.getMessage());
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("====> [Error] 삭제 실패: ", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Delete Failed"));
        }
    }

    /**
     * 게시글 전체 조회 (카테고리 필터)
     */
    @GetMapping("/list")
    public ResponseEntity<List<BoardDTO>> getList(@RequestParam(name = "category", defaultValue = "전체") String category) {
        log.info("====> [MSA Board] 목록 조회 호출: {}", category);
        try {
            List<BoardDTO> list = boardService.getBoardList(category);
            return ResponseEntity.ok(list != null ? list : new ArrayList<>());
        } catch (Exception e) {
            log.error("====> [Error] 목록 조회 실패: ", e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * 게시글 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<BoardDTO> getDetail(@PathVariable("id") Long id) {
        log.info("====> [MSA Board] 상세 조회 호출 ID: {}", id);
        try {
            BoardDTO detail = boardService.getBoardDetail(id);
            if (detail == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            log.error("====> [Error] 상세 조회 실패: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}