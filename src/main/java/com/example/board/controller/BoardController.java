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

        // 401 에러 방지를 위한 유저 검증 추가
    if (loginUser == null) {
        log.error("====> [작성 실패] 인증 정보가 없습니다.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
        log.info("====> [게시글 작성] MemberID: {}, 파일유무: {}", 
                loginUser.getMemberId(), (file != null && !file.isEmpty()));
        BoardResponseDTO response = boardService.writeBoard(request, file, loginUser.getMemberId());
        return ResponseEntity.ok(response);
    }

    // 4. 게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<BoardResponseDTO> delete(
        @LoginUser RedisMemberDTO loginUser,
        @PathVariable(name = "id") Long id) {

        if (loginUser == null) {
            log.error("====> [삭제 실패] 인증 정보가 없습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("===> 게시글 삭제요청: ID ={}, 요청자 ={}, 권한 ={}",
                id, loginUser.getMemberId(), loginUser.getRole());

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

        // 로그인 유저 검증 추가
        if (loginUser == null) {
            log.error("====> [수정 실패] 인증 정보가 없습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("===> 게시글 수정요청: ID ={}, 요청자 ={}, 권한 ={}", 
                id, loginUser.getMemberId(), loginUser.getRole());
                
        // 서비스 호출
        BoardResponseDTO response = boardService.updateBoard(id, request, file, loginUser.getMemberId(), loginUser.getRole());

        return ResponseEntity.ok(response);
    }
}