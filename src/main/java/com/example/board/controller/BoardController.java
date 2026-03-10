package com.example.board.controller;

import com.example.board.service.BoardService;
import com.example.board.dto.BoardDTO;
import com.example.board.dto.BoardCreateRequest;
import com.example.board.dto.BoardResponseDTO;
import com.example.common.annotation.LoginUser;
import com.example.member.dto.RedisMemberDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // 1. 게시글 목록 조회
    @GetMapping("/list")
    public ResponseEntity<List<BoardDTO>> getList(@RequestParam(name = "category", required = false) String category) {
        log.info("====> [목록 조회] 카테고리: {}", category);
        List<BoardDTO> list = boardService.getBoardList(category);
        return ResponseEntity.ok(list);
    }

    // 2. 게시글 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<BoardDTO> getDetail(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(boardService.getBoardDetail(id));
    }

    // 3. 게시글 작성 (파일 포함)
    @PostMapping(value = "/write", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<BoardResponseDTO> write(
            @LoginUser RedisMemberDTO loginUser,
            @RequestPart("request") BoardCreateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

       if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        BoardResponseDTO response = boardService.writeBoard(request, file, loginUser.getMemberId());
        return ResponseEntity.ok(response);
    }

    // 4. 게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<BoardResponseDTO> delete(
        @LoginUser RedisMemberDTO loginUser,
        @PathVariable(name = "id") Long id) {

        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        BoardResponseDTO response = boardService.deleteBoard(id, loginUser.getMemberId(), loginUser.getRole());
        return ResponseEntity.ok(response);
    }

    
    // 5. 게시글 수정
    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<BoardResponseDTO> update(
        @LoginUser RedisMemberDTO loginUser,
        @PathVariable(name = "id") Long id,
        @RequestPart("request") BoardCreateRequest request,
        @RequestPart(value = "file", required = false) MultipartFile file) throws Exception {

       if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                
        BoardResponseDTO response = boardService.updateBoard(id, request, file, loginUser.getMemberId(), loginUser.getRole());
        return ResponseEntity.ok(response);
    }

    // 6. 하트 클릭 (좋아요 토글)
    @PostMapping("/{id}/like")
    public ResponseEntity<Integer> toggleLike(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "id") Long id) {
        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        int updatedLikeCount = boardService.toggleLike(id, loginUser.getMemberId());
        return ResponseEntity.ok(updatedLikeCount);
    }

    // 7. 댓글 작성
    @PostMapping("/{id}/comment")
    public ResponseEntity<Void> addComment(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "id") Long id,
            @RequestBody Map<String, String> payload) { // JSON의 "content" 필드를 안전하게 받기 위함
        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        String content = payload.get("content");
        // 댓글 저장 후 업데이트된 총 댓글 수를 반환
        int updatedCommentCount = boardService.addComment(id, loginUser.getMemberId(), content);
        return ResponseEntity.ok(updatedCommentCount);
    }
}