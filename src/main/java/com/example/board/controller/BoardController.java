package com.example.board.controller;

import com.example.board.service.BoardCommentService;
import com.example.board.service.BoardReportService;
import com.example.board.service.BoardService;
import com.example.board.dto.*;
import com.example.common.annotation.LoginUser;
import com.example.member.dto.RedisMemberDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final BoardCommentService boardCommentService;
    private final BoardReportService boardReportService;

    @Value("${file.upload.dir}")
    private String uploadDir;

    // --- [1. 게시글 관련 API] ---

    @GetMapping("/list")
    public ResponseEntity<List<BoardDTO>> getList(@RequestParam(name = "category", required = false) String category) {
        log.info("====> [목록 조회] 카테고리: {}", category);
        List<BoardDTO> list = boardService.getBoardList(category);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardDTO> getDetail(
        @LoginUser RedisMemberDTO loginUser, 
        @PathVariable(name = "id") Long id) {
        
        BoardDTO detail = boardService.getBoardDetail(id);

        // [중요] 응답을 보내기 전에 로그를 찍어보면 누가 요청했는지 알 수 있습니다.
    log.info("조회 요청자 ID: {}", loginUser != null ? loginUser.getMemberId() : "비로그인");
    
    return ResponseEntity.ok(detail);
}

    @PostMapping(value = "/write", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<BoardResponseDTO> write(
            @LoginUser RedisMemberDTO loginUser,
            @RequestPart("request") BoardCreateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        BoardResponseDTO response = boardService.writeBoard(request, file, loginUser.getMemberId());
        return ResponseEntity.ok(response);
    }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<BoardResponseDTO> delete(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "id") Long id) {

        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        BoardResponseDTO response = boardService.deleteBoard(id, loginUser.getMemberId(), loginUser.getRole());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Integer> toggleLike(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "id") Long id) {

        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        int updatedLikeCount = boardService.toggleLike(id, loginUser.getMemberId());
        return ResponseEntity.ok(updatedLikeCount);
    }

    // --- [2. 댓글 관련 API] ---

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponseDTO> addComment(
            @PathVariable(name = "id") Long boardId,
            @LoginUser RedisMemberDTO loginUser,
            @RequestBody CommentRequestDTO request) {

        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        request.setBoardId(boardId);
        request.setMemberId(loginUser.getMemberId());
        
        CommentResponseDTO response = boardCommentService.createComment(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponseDTO>> getComments(@PathVariable(name = "id") Long id) {
        List<CommentResponseDTO> comments = boardCommentService.getCommentsByBoardId(id);
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "commentId") Long commentId) {
            
        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        boardCommentService.deleteComment(commentId, loginUser.getMemberId(), loginUser.getRole());
        return ResponseEntity.noContent().build();
    }
    
    // --- [3. 파일 제공 API] ---

    @GetMapping("/files/{fileName}")
    public ResponseEntity<Resource> serveFile(@PathVariable(name = "fileName") String fileName) {
        try {
            Path path = Paths.get(uploadDir).resolve(fileName);
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // --- [4. 신고 및 관리자 API는 기존과 동일하게 유지하되 가독성 개선] ---

    @PostMapping("/{id}/report")
    public ResponseEntity<String> reportBoard(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "id") Long id,
            @RequestBody Map<String, String> body) {

        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        String result = boardReportService.reportBoard(id, loginUser.getMemberId(), body.get("reason"));
        if ("ALREADY_REPORTED".equals(result)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 신고한 게시글입니다.");
        }
        return ResponseEntity.ok("신고가 접수되었습니다.");
    }

    @PostMapping("/comments/{commentId}/report")
    public ResponseEntity<String> reportComment(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "commentId") Long commentId,
            @RequestBody Map<String, String> body) {

        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        String result = boardReportService.reportComment(commentId, loginUser.getMemberId(), body.get("reason"));
        if ("ALREADY_REPORTED".equals(result)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 신고한 댓글입니다.");
        }
        return ResponseEntity.ok("댓글 신고가 접수되었습니다.");
    }

    @GetMapping("/admin/reports/boards")
    public ResponseEntity<?> getBoardReports(@LoginUser RedisMemberDTO loginUser) {
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(boardReportService.getBoardReportList());
    }

    @GetMapping("/admin/reports/comments")
    public ResponseEntity<?> getCommentReports(@LoginUser RedisMemberDTO loginUser) {
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(boardReportService.getCommentReportList());
    }

    @PutMapping("/admin/reports/boards/{reportId}/approve")
    public ResponseEntity<String> approveBoardReport(
            @LoginUser RedisMemberDTO loginUser, 
            @PathVariable(name = "reportId") Long reportId) {
            
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boardReportService.approveBoardReport(reportId);
        return ResponseEntity.ok("게시글 신고가 승인되었습니다.");
    }

    @PutMapping("/admin/reports/comments/{reportId}/approve")
    public ResponseEntity<String> approveCommentReport(
            @LoginUser RedisMemberDTO loginUser, 
            @PathVariable(name = "reportId") Long reportId) {
            
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boardReportService.approveCommentReport(reportId);
        return ResponseEntity.ok("댓글 신고가 승인되었습니다.");
    }
}