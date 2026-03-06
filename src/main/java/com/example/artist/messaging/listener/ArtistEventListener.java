package com.example.artist.messaging.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.artist.dto.PaymentResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArtistEventListener {

	// pay로부터 처리 결과를 수신
	@RabbitListener(queues = "artist.payment.reply.queue")
	public void handlePaymentResult(PaymentResponseDTO response) {
		log.info("=====> [Artist 서버 수신] 주문번호: {}, 상태: {}, 메시지: {}", 
                response.getOrderId(), response.getStatus(), response.getMessage());
		
		if ("COMPLETE".equals(response.getStatus())) {
		    log.info(">>>>>> 주문번호 {} 후원 처리 완료!", response.getOrderId());
		} else if ("PROCESSING".equals(response.getStatus())) {
		    log.info(">>>>>> 주문번호 {} 결제 진행 중...", response.getOrderId());
		} else if ("FAIL".equals(response.getStatus())) {
		    log.error(">>>>>> 주문번호 {} 후원 실패: {}", response.getOrderId(), response.getMessage());
		}
	}
	
}
