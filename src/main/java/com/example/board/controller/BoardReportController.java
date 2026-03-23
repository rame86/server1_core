package com.example.board.controller;

import com.example.board.service.BoardReportService;
import com.example.common.annotation.LoginUser;
import com.example.member.dto.RedisMemberDTO;
import com.example.admin.dto.ReportBoardDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/board/admin") // 로그에 찍힌 경로 순서에 맞춰서 수정했습니다!
@RequiredArgsConstructor
public class BoardReportController {

    private final BoardReportService boardReportService;

    /**
     * 게시글 신고 목록 조회
     * 요청 경로: GET /board/admin/reports
     */
    @GetMapping("/reports")
    public ResponseEntity<List<ReportBoardDTO>> getBoardReports(@LoginUser RedisMemberDTO loginUser) {
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("-----> [Admin] 게시글 신고 목록 조회 요청 수신");
        return ResponseEntity.ok(boardReportService.getBoardReportList());
    }

    /**
     * 댓글 신고 목록 조회
     * 요청 경로: GET /board/admin/reports/comments
     */
    @GetMapping("/reports/comments")
    public ResponseEntity<List<ReportBoardDTO>> getCommentReports(@LoginUser RedisMemberDTO loginUser) {
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("-----> [Admin] 댓글 신고 목록 조회 요청 수신");
        return ResponseEntity.ok(boardReportService.getCommentReportList());
    }

    /**
     * 게시글 신고 승인
     * 요청 경로: PUT /board/admin/report/{reportId}/approve
     */
    @PutMapping("/report/{reportId}/approve")
    public ResponseEntity<Void> approveBoardReport(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "reportId") Long reportId,
            @RequestBody Map<String, Long> body) {

        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long boardId = body.get("boardId");
        boardReportService.approveBoardReport(reportId, boardId);
        return ResponseEntity.ok().build();
    }

    /**
     * 댓글 신고 승인
     * 요청 경로: PUT /board/admin/report/comment/{reportId}/approve
     */
    @PutMapping("/report/comment/{reportId}/approve")
    public ResponseEntity<Void> approveCommentReport(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "reportId") Long reportId) {

        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boardReportService.approveCommentReport(reportId);
        return ResponseEntity.ok().build();
    }

    /**
     * 신고 내역 삭제
     * 요청 경로: DELETE /board/admin/reports/boards/{reportId}
     */
    @DeleteMapping("/reports/boards/{reportId}")
    public ResponseEntity<Void> deleteBoardReport(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "reportId") Long reportId) {

        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boardReportService.deleteBoardReport(reportId);
        return ResponseEntity.ok().build();
    }
}