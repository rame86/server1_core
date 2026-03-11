package com.example.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.dto.AdminEventListDTO;
import com.example.admin.dto.EventResultDTO;
import com.example.admin.dto.ReportBoardDTO;
import com.example.admin.dto.ShopResultDTO;
import com.example.admin.service.AdminService;
import com.example.board.entity.ReportBoard;
import com.example.common.annotation.LoginUser;
import com.example.config.RabbitMQConfig;
import com.example.member.dto.RedisMemberDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestParam;


@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
	
	private final AdminService adminService;
	
	@PostMapping("/event/confirm")
	public ResponseEntity<String> confirmEvent(@RequestBody EventResultDTO dto, @LoginUser RedisMemberDTO user) {
		log.info("=====> [1서버 관리자] 결정 전송 요청: {}", dto);
		adminService.processApproval(dto, RabbitMQConfig.EVENT_RES_ROUTING_KEY, user.getMemberId());
		return ResponseEntity.ok("EVENT 처리 완료");
	}
	
	@PostMapping("/shop/confirm")
	public ResponseEntity<String> confirmShop(@RequestBody ShopResultDTO dto, @LoginUser RedisMemberDTO user) {
		log.info("=====> [1서버 관리자] 결정 전송 요청: {}", dto);
		adminService.processApproval(dto, RabbitMQConfig.SHOP_RES_ROUTING_KEY, user.getMemberId());
		return ResponseEntity.ok("SHOP 처리 완료");
	}
	
	@GetMapping("/event/list")
	public ResponseEntity<List<AdminEventListDTO>> getEventList(){
		log.info("=====> [1서버 관리자] 전체 이벤트 리스트 조회 요청");
		List<AdminEventListDTO> list = adminService.getAllEvents();
		return ResponseEntity.ok(list);
	}
	
	// 게시글 신고 추가
	@GetMapping("/board/reports")
	public ResponseEntity<List<ReportBoardDTO>> getBoardReportList(@LoginUser RedisMemberDTO user) {
		log.info("=====> [1서버 관리자] 게시글 신고 리스트 조회 요청자: {}", user.getMemberId());
		
		// 서비스에서 DTO 리스트를 반환하므로 타입을 맞춰줍니다.
		List<ReportBoardDTO> list = adminService.getBoardReports(); 
		return ResponseEntity.ok(list);
	}
	
}
