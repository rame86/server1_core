package com.example.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.service.AdminSettlementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/admin/settlement")
@RequiredArgsConstructor
public class AdminSettlementController {
	
	private final AdminSettlementService adminSettlementService;
	
	@GetMapping
	public ResponseEntity<String> getSettlementStats() {
	    log.info("=====> [core 서비스 관리자] 통합 대시보드 데이터 요청 접수");
	    adminSettlementService.requestDashboardData();
	    return ResponseEntity.ok("정산 데이터 요청이 성공적으로 접수되었습니다.");
	}

}
