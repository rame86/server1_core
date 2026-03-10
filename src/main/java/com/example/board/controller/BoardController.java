package com.example.board.controller;

import com.example.board.service.BoardService;
import com.example.board.dto.BoardDTO;
import com.example.common.annotation.LoginUser;
import com.example.member.dto.RedisMemberDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    
    /**
     *게시글 작성: @LoginUser를 통해 Redis에서 인증된 사용자 정보를 자동으로 주입받습니다.
     */
    @PutMapping(value="/write", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> write(
            @LoginUser RedisMemberDTO loginUser, 
            @RequestPart("board") BoardDTO boardDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        log.info("====> [MSA Board] 게시글 작성 호출: 사용자={}, 제목={}", loginUser.getMemberId(), boardDTO.getTitle());
        try {
            // @LoginUser가 주입되었다는 것은 이미 인증이 완료되었음을 보장합니다.
            boardDTO.setMemberId(loginUser.getMemberId());
            boardService.writeBoard(boardDTO, file);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Success"));
        } catch (Exception e) {
            log.error("====> [Error] 게시글 작성 실패: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "게시글 작성 중 오류가 발생했습니다."));
        }
    }
    /**
     * 게시글 수정
     */
    @PutMapping("/update")
    public ResponseEntity<?> update(
            @LoginUser RedisMemberDTO loginUser,
            @RequestBody BoardDTO boardDTO) {
        
        log.info("====> [MSA Board] 게시글 수정 호출 id: {}", boardDTO.getBoardId());
        try {
            boardService.updateBoard(boardDTO, loginUser.getMemberId());
            return ResponseEntity.ok(Map.of("message", "Update Success"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("====> [Error] 수정 실패: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Update Failed"));
        }
    }

    /**
     * 게시글 삭제
     */
   @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable("id") Long id) {
        
       // 리졸버가 null을 반환했을 경우 처리
    if (loginUser == null) {
        log.warn("====> [Auth] 삭제 실패: 인증 정보가 없습니다.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
    }
    
    log.info("====> [Board] 삭제 호출 id: {}, 요청자: {}", id, loginUser.getMemberId());
    try {
        boardService.deleteBoard(id, loginUser.getMemberId());
        return ResponseEntity.ok(Map.of("message", "Delete Success"));
    } catch (IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
    } catch (Exception e) {
        log.error("====> [Error] 삭제 실패: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Delete Failed"));
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
        }*/
    @GetMapping("/{id}")
        public ResponseEntity<BoardDTO> getDetail(@PathVariable("id") Long id) {
            BoardDTO detail = boardService.getBoardDetail(id);
            return detail != null ? ResponseEntity.ok(detail) : ResponseEntity.notFound().build();
        }
}