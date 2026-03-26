package com.example.admin.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.admin.dto.AdminRefundRequestDTO;
import com.example.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminRefundListener {
	
	@RabbitListener(queues = RabbitMQConfig.REFUND_REQ_QUEUE_NAME)
	public void receiveRefundRequest(AdminRefundRequestDTO dto) {
		log.info("=================================================");
        log.info("🚀 [Admin 서버] 새로운 환불 요청이 도착했습니다!");
        log.info("카테고리: {}", dto.getCategory());
        log.info("타겟 ID (예약/주문번호): {}", dto.getTargetId());
        log.info("제목: {}", dto.getTitle());
        log.info("환불 금액: {}", dto.getTotalPrice());
        log.info("상세 데이터 (JSON): {}", dto.getContentJson());
        log.info("=================================================");
	}

}
