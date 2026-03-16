package com.example.board.controller;

import com.example.board.service.BoardService;
import com.example.board.dto.BoardDTO;
import com.example.board.dto.BoardCreateRequest;
import com.example.board.dto.BoardResponseDTO;
import com.example.board.dto.CommentRequestDTO;
import com.example.board.dto.CommentResponseDTO;
import com.example.board.entity.Comment;
import com.example.board.entity.BoardReport;
import com.example.board.repository.ReportRepository;
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

    // 게시글 목록 조회
    @GetMapping("/list")
    public ResponseEntity<List<BoardDTO>> getList(@RequestParam(name = "category", required = false) String category) {
        log.info("====> [목록 조회] 카테고리: {}", category);
        List<BoardDTO> list = boardService.getBoardList(category);
        return ResponseEntity.ok(list);
    }

    // 게시글 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<BoardDTO> getDetail(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(boardService.getBoardDetail(id));
    }

    // 게시글 작성 (파일 포함)
    @PostMapping(value = "/write", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<BoardResponseDTO> write(
            @LoginUser RedisMemberDTO loginUser,
            @RequestPart("request") BoardCreateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

       if (loginUser == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
       }
        BoardResponseDTO response = boardService.writeBoard(request, file, loginUser.getMemberId());
        return ResponseEntity.ok(response);
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<BoardResponseDTO> delete(
        @LoginUser RedisMemberDTO loginUser,
        @PathVariable(name = "id") Long id) {

        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        BoardResponseDTO response = boardService.deleteBoard(id, loginUser.getMemberId(), loginUser.getRole());
        return ResponseEntity.ok(response);
    }

    // 게시글 수정
    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<BoardResponseDTO> update(
        @LoginUser RedisMemberDTO loginUser,
        @PathVariable(name = "id") Long id,
        @RequestPart("request") BoardCreateRequest request,
        @RequestPart(value = "file", required = false) MultipartFile file) throws Exception {

       if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
       }        
        BoardResponseDTO response = boardService.updateBoard(id, request, file, loginUser.getMemberId(), loginUser.getRole());
        return ResponseEntity.ok(response);
    }

    // 댓글 작성
    @PostMapping("/{id}/comments")
    public ResponseEntity<Integer> addComment(
        @PathVariable(name= "id") Long boardId,
        @LoginUser RedisMemberDTO loginUser,
        @RequestBody CommentRequestDTO request){

        if(loginUser == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        int updatedCommentCount = boardService.addComment(boardId, loginUser.getMemberId(), request.getContent());
        return ResponseEntity.ok(updatedCommentCount);
    }
    // 댓글 목록 조회 
    @GetMapping("/{id}/comments")
   public ResponseEntity<List<CommentResponseDTO>> getComments(@PathVariable(name = "id") Long id) {
        List<CommentResponseDTO> comments = boardService.getComments(id);
        return ResponseEntity.ok(comments);
    }

    // 하트 클릭 (좋아요 토글)
    @PostMapping("/{id}/like")
    public ResponseEntity<Integer> toggleLike(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "id") Long id) {

        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        int updatedLikeCount = boardService.toggleLike(id, loginUser.getMemberId());
        return ResponseEntity.ok(updatedLikeCount);
    }

    // 게시글 신고
    @PostMapping("/{id}/report")
    public ResponseEntity<String> reportBoard(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "id") Long id,
            @RequestBody Map<String, String> body) { // JSON으로 사유를 받음

        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String reason = body.get("reason");
        String result = boardService.reportBoard(id, loginUser.getMemberId(), reason);

        if ("ALREADY_REPORTED".equals(result)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 신고한 게시글입니다.");
        }

        return ResponseEntity.ok("신고가 접수되었습니다.");
    }
       // [관리자] 게시글 신고 목록 조회
    @GetMapping("/admin/reports/boards")
    public ResponseEntity<?> getBoardReports(@LoginUser RedisMemberDTO loginUser) {
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(boardService.getBoardReportList());
    }

    // [관리자] 댓글 신고 목록 조회
    @GetMapping("/admin/reports/comments")
    public ResponseEntity<?> getCommentReports(@LoginUser RedisMemberDTO loginUser) {
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(boardService.getCommentReportList());
    }

    // [관리자] 게시글 신고 승인
    @PutMapping("/admin/reports/boards/{reportId}/approve")
    public ResponseEntity<String> approveBoardReport(@LoginUser RedisMemberDTO loginUser, @PathVariable(name = "reportId") Long reportId) {
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        boardService.approveReport(reportId);
        return ResponseEntity.ok("게시글 신고가 승인되었습니다.");
    }

    // [관리자] 댓글 신고 승인
    @PutMapping("/admin/reports/comments/{reportId}/approve")
    public ResponseEntity<String> approveCommentReport(@LoginUser RedisMemberDTO loginUser, @PathVariable(name = "reportId") Long reportId) {
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        boardService.approveCommentReport(reportId);
        return ResponseEntity.ok("댓글 신고가 승인되었습니다.");
    }
}