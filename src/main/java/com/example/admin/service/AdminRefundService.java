package com.example.admin.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.example.admin.dto.AdminRefundResponseDTO;
import com.example.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRefundService {
	
	private final RabbitTemplate rabbitTemplate;
	
	// 관리자가 승인/반려 버튼을 눌렀을 때 호출
	public void approveRefund(AdminRefundResponseDTO dto) {
		log.info("----->환불 처리 결과 전송 시작: {}", dto);
		
		rabbitTemplate.convertAndSend(
				RabbitMQConfig.EXCHANGE_NAME,
				RabbitMQConfig.REFUND_RES_ROUTING_KEY,
				dto);
		
		log.info("----->환불 처리 결과 전송 완료: {}", dto.getTargetId());
	}
}
