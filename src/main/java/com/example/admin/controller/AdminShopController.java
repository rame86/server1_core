package com.example.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.dto.approval.ShopResultDTO;
import com.example.admin.service.AdminApprovalService;
import com.example.admin.service.AdminShopService;
import com.example.common.annotation.LoginUser;
import com.example.config.RabbitMQConfig;
import com.example.member.dto.RedisMemberDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/admin/shop")
@RequiredArgsConstructor
public class AdminShopController {
	
	private final AdminShopService adminShopService;
	private final AdminApprovalService adminApprovalService;
	
	@PostMapping("/confirm")
	public ResponseEntity<String> confirmShop(@RequestBody ShopResultDTO dto, @LoginUser RedisMemberDTO user) {
		log.info("=====> [1서버 관리자] 결정 전송 요청: {}", dto);
		adminApprovalService.processApproval(dto, RabbitMQConfig.SHOP_RES_ROUTING_KEY, user.getMemberId());
		return ResponseEntity.ok("SHOP 처리 완료");
	}
	
	// 샵 승인 대기 목록
	@GetMapping("/approvalList")
	public ResponseEntity<List<ShopResultDTO>> getPendingShop() {
		List<ShopResultDTO> pendingList = adminShopService.getPendingShopList("SHOP", "PENDING");
		return ResponseEntity.ok(pendingList);
	}

}
