package com.example.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.dto.event.AdminEventListDTO;
import com.example.admin.dto.event.EventResultDTO;
import com.example.admin.service.AdminApprovalService;
import com.example.admin.service.AdminEventService;
import com.example.common.annotation.LoginUser;
import com.example.config.RabbitMQConfig;
import com.example.member.dto.RedisMemberDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/admin/event")
@RequiredArgsConstructor
public class AdminEventController {
	
	private final AdminEventService adminEventService;
    private final AdminApprovalService adminApprovalService;
	
	@PostMapping("/event/confirm")
	public ResponseEntity<String> confirmEvent(@RequestBody EventResultDTO dto, @LoginUser RedisMemberDTO user) {
		log.info("=====> [1서버 관리자] 결정 전송 요청: {}", dto);
		adminApprovalService.processApproval(dto, RabbitMQConfig.EVENT_RES_ROUTING_KEY, user.getMemberId());
		return ResponseEntity.ok("EVENT 처리 완료");
	}
	
	@GetMapping("/event/list")
	public ResponseEntity<List<AdminEventListDTO>> getEventList() {
		log.info("=====> [1서버 관리자] 전체 이벤트 리스트 조회 요청");
		List<AdminEventListDTO> list = adminEventService.getAllEvents();
		return ResponseEntity.ok(list);
	}

}
