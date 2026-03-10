package com.example.admin.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.dto.EventResultDTO;
import com.example.admin.dto.ShopResultDTO;
import com.example.admin.service.AdminService;
import com.example.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
	
	private final RabbitTemplate rabbitTemplate;
	private final AdminService adminService;
	
	@PostMapping("/event/confirm")
	public ResponseEntity<String> confirmEvent(@RequestBody EventResultDTO dto) {
		log.info("=====> [1서버 관리자] 결정 전송 요청: {}", dto);
		try {
			// DB에 상태 업데이트하기
			adminService.updateApprovalStatus(dto.getApprovalId(), dto.getStatus());
			
			// 2서버로 쏘기
			rabbitTemplate.convertAndSend(
					RabbitMQConfig.EXCHANGE_NAME, 
					RabbitMQConfig.EVENT_RES_ROUTING_KEY, 
					dto);
			log.info("=====> [1서버] 2서버로 메시지 전송 성공!");
			return ResponseEntity.ok("2서버로 결정 사항을 전달했습니다: " + dto.getStatus());
		} catch(Exception e) {
			log.error("=====> [1서버] 메시지 전송 실패: {}", e.getMessage());
			return ResponseEntity.internalServerError().body("전송 중 오류 발생: " + e.getMessage());
		}
	}
	
	@PostMapping("/shop/confirm")
	public ResponseEntity<String> confirmShop(@RequestBody ShopResultDTO dto) {
		log.info("=====> [1서버 관리자] 결정 전송 요청: {}", dto);
		try {
			adminService.updateApprovalStatus(dto.getApprovalId(), dto.getStatus());
			rabbitTemplate.convertAndSend(
					RabbitMQConfig.EXCHANGE_NAME, 
					RabbitMQConfig.SHOP_RES_ROUTING_KEY, 
					dto);
			log.info("=====> [1서버] 2서버로 메시지 전송 성공!");
			return ResponseEntity.ok("2서버로 결정 사항을 전달했습니다: " + dto.getStatus());
		} catch(Exception e) {
			log.error("=====> [1서버] 메시지 전송 실패: {}", e.getMessage());
			return ResponseEntity.internalServerError().body("전송 중 오류 발생: " + e.getMessage());
		}
	}
	
}
