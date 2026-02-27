package com.example.board.controller;

import com.example.board.dto.BoardDTO;
import com.example.board.service.BoardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/msa/core/api/board")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class BoardController {

    private final BoardService boardService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 게시글 목록 조회 (카테고리별 캐싱 고려)
     */
    @GetMapping("/posts")
    public List<BoardDTO> getList(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "전체") String category) {
        
        List<BoardDTO> list = boardService.getBoardList(category);
        BoardDTO loginUser = getLoginUserInfo(authHeader);

        for (BoardDTO post : list) {
            if (post.getAuthorName() == null || post.getAuthorName().isBlank()) {
                post.setAuthorName("사용자");
            }

            // 본인 작성글 식별 및 제목 강조
            if (loginUser != null && Objects.equals(post.getMemberId(), loginUser.getMemberId())) {
                post.setTitle("[내 글] " + post.getTitle());
                post.setAuthorName(loginUser.getAuthorName());
            }
        }
        return list;
    }

    /**
     * 게시글 저장 로직
     * 405 Method Not Allowed 방지를 위해 @PostMapping("/posts") 명시
     */
    @PostMapping("/posts") 
    public ResponseEntity<?> write(@RequestBody BoardDTO boardDTO, 
                                 @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Redis 세션에서 로그인 정보 획득
            BoardDTO loginUser = getLoginUserInfo(authHeader);
            
            if (loginUser == null) {
                log.warn("미인증 사용자의 글쓰기 시도");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요한 서비스입니다.");
            }

            // DTO에 인증된 정보 강제 주입 (보안 강화)
            boardDTO.setMemberId(loginUser.getMemberId());
            boardDTO.setAuthorName(loginUser.getAuthorName());
            boardDTO.setAuthorRole(loginUser.getAuthorRole());
            
            // 서비스 호출 및 DB 저장
            boardService.writeBoard(boardDTO);
            
            // 등록 성공 시 특정 카테고리의 Redis 캐시를 비우는 로직이 Service에 포함되어야 함
            log.info("글 등록 완료: {}", boardDTO.getTitle());
            return ResponseEntity.status(HttpStatus.CREATED).body("등록되었습니다.");
            
        } catch (Exception e) {
            log.error("글 작성 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * Redis 기반 토큰 검증 및 사용자 정보 추출 (MSA 공통 로직)
     */
    private BoardDTO getLoginUserInfo(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        
        try {
            String token = authHeader.substring(7);
            // Auth 서비스에서 저장한 Redis 키 "TOKEN:{jwt}" 조회
            String redisValue = redisTemplate.opsForValue().get("TOKEN:" + token);
            
            if (redisValue != null) {
                Map<String, Object> map = objectMapper.readValue(redisValue, Map.class);
                
                BoardDTO user = new BoardDTO();
                user.setMemberId(Long.parseLong(String.valueOf(map.get("member_id"))));
                user.setAuthorName(String.valueOf(map.get("name")));
                user.setAuthorRole(String.valueOf(map.get("role")));
                
                return user;
            }
        } catch (Exception e) {
            log.error("Redis 세션 파싱 에러: {}", e.getMessage());
        }
        return null;
    }
}