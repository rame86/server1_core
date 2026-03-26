package com.example.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.dto.refund.AdminRefundResponseDTO;
import com.example.admin.service.AdminRefundService;
import com.example.common.annotation.LoginUser;
import com.example.member.dto.RedisMemberDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/admin/refund")
@RequiredArgsConstructor
public class AdminRefundController {
	
	private final AdminRefundService adminRefundService;
	
	@PostMapping
	public ResponseEntity<String> approveRefund(@RequestBody AdminRefundResponseDTO dto, @LoginUser RedisMemberDTO user) {
		log.info("=====> [1서버 관리자] 결정 전송 요청: {}", dto);
		if(user.getMemberId() != null) dto.setAdminId(user.getMemberId());
		adminRefundService.approveRefund(dto);
		return ResponseEntity.ok("REFUND 처리 완료");
	}

}
