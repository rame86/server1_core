package com.example.board.controller;

import com.example.board.service.BoardReportService;
import com.example.common.annotation.LoginUser;
import com.example.member.dto.RedisMemberDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardReportController {

    private final BoardReportService boardReportService;

    // --- [1. 신고 접수 API] ---

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

    @PutMapping("/comments/{reportId}/approve")
    public ResponseEntity<Void> approveCommentReport(@PathVariable(name = "reportId") Long reportId) {
        log.info("-----> [Board 서비스] 댓글 신고 승인 요청 수신: reportId={}", reportId);
        
        try {
            // 여기에 실제 DB의 댓글 상태를 변경하거나 숨기는 로직을 넣으세요.
            // 예: commentService.hideCommentByReport(reportId);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("-----> [Board 서비스] 댓글 승인 처리 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }  
        // --- [2. 어드민 전용 API] ---

    @GetMapping("/admin/reports/boards")
    public ResponseEntity<?> getBoardReports(@LoginUser RedisMemberDTO loginUser) {
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(boardReportService.getBoardReportList());
    }

    @DeleteMapping("/admin/reports/boards/{reportId}")
    public ResponseEntity<Void> deleteBoardReport(@PathVariable(name = "reportId") Long reportId) {
        log.info("-----> [BoardReportController] 신고 내역 삭제 요청 수신: reportId={}", reportId);
        boardReportService.deleteBoardReport(reportId);
        return ResponseEntity.ok().build();
    }
}