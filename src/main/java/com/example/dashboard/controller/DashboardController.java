package com.example.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.dashboard.service.UserDashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserDashboardService userDashboardService;

    /**
     * 유저 대시보드 로드 시 호출되는 엔드포인트.
     * Pay 서비스에 MQ로 유저 결제/포인트 데이터를 요청.
     * Pay 서비스는 응답을 user.dashboard.pay.res 큐로 보내고,
     * UserDashboardPayListener가 받아 STOMP /topic/user-dashboard/{memberId}로 전달.
     */
    @PostMapping("/dashboard-queue")
    public ResponseEntity<String> triggerDashboardQueue(@RequestBody Map<String, Object> request) {
        try {
            Object memberIdRaw = request.get("memberId");
            if (memberIdRaw == null) {
                return ResponseEntity.badRequest().body("memberId가 필요합니다.");
            }

            Long memberId = Long.valueOf(String.valueOf(memberIdRaw));
            userDashboardService.requestUserDashboardData(memberId);

            return ResponseEntity.ok("대시보드 데이터 요청 완료. 웹소켓으로 수신 대기 중.");
        } catch (Exception e) {
            log.error("[DashboardController] 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("오류: " + e.getMessage());
        }
    }
}
