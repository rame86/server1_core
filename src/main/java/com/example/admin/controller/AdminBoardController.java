package com.example.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.admin.service.AdminBoardService;
import com.example.admin.dto.ReportBoardDTO;
import com.example.common.annotation.LoginUser;
import com.example.member.dto.RedisMemberDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("admin/board")
@RequiredArgsConstructor
public class AdminBoardController {

    private final AdminBoardService adminBoardService;

    //1. 신고 목록 조회 GET /msa/admin/board/reports
    @GetMapping("/reports")
    public ResponseEntity<List<ReportBoardDTO>> getReports(@LoginUser RedisMemberDTO loginUser) {
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(adminBoardService.getBoardReports());
    }

    // 2. 댓글 신고 목록 조회 (GET /admin/board/reports/comments)
    @GetMapping("/reports/comments")
    public ResponseEntity<List<ReportBoardDTO>> getCommentReports(@LoginUser RedisMemberDTO loginUser) {
        log.info("-----> [Admin] 댓글 신고 목록 조회 요청 수신");
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) {
            log.warn("-----> [Admin] 권한 없음 또는 로그인 정보 없음");
            return ResponseEntity.status(403).build();
        }
        
        List<ReportBoardDTO> reports = adminBoardService.getCommentReports();
        log.info("-----> [Admin] 댓글 신고 목록 반환 개수: {}", reports.size());
        return ResponseEntity.ok(reports);
    }
    
    // 게시글 신고 승인 (숨김 처리)  PUT /msa/core/admin/board/report/{reportId}/approve
    @PutMapping("/report/{reportId}/approve")
    public ResponseEntity<Void> approveReport(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "reportId") Long reportId,
            @RequestBody java.util.Map<String, Long> body) {
        
        log.info("-----> [AdminBoardController] 승인 요청 수신: reportId={}, boardId={}", reportId, body.get("boardId"));
        
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) {
            return ResponseEntity.status(403).build();
        }
        
        Long boardId = body.get("boardId");
        adminBoardService.approveBoardReport(reportId, boardId, loginUser.getMemberId());
        
        // [중요] 성공 시 200 OK 응답을 확실히 반환하여 리액트의 catch문으로 빠지지 않게 합니다.
        return ResponseEntity.ok().build();
    }

    // 4. 댓글 신고 승인 (PUT /admin/board/report/comment/{reportId}/approve)
    @PutMapping("/report/comment/{reportId}/approve")
    public ResponseEntity<Void> approveCommentReport(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "reportId") Long reportId,
            @RequestBody Map<String, Long> body) {
        
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) {
            return ResponseEntity.status(403).build();
        }

        log.info("-----> [Admin] 댓글 승인 요청: reportId={}", reportId);
        adminBoardService.approveCommentReport(reportId);
        
        return ResponseEntity.ok().build();
    }
    
    // 신고 내역 삭제 (게시글/댓글 공용 또는 분리 가능) DELETE /msa/admin/board/report/{reportId}
    @DeleteMapping("/report/{reportId}")
    public ResponseEntity<Void> deleteReport(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "reportId") Long reportId) {
        
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) {
            return ResponseEntity.status(403).build();
        }

        adminBoardService.deleteBoardReport(reportId);
        return ResponseEntity.ok().build();
    }
}