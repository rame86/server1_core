package com.example.artist.messaging.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.example.artist.dto.PaymentRequestDTO;
import com.example.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArtistEventProducer {
	
	private final RabbitTemplate rabbitTemplate;
	
	public void sendPaymentRequest(PaymentRequestDTO request) {
		log.info(">>>> [MQ 전송] 주문번호: {}", request.getOrderId());
		rabbitTemplate.convertAndSend(
				RabbitMQConfig.EXCHANGE_NAME,
				RabbitMQConfig.PAY_ROUTING_KEY,
				request);
	}
	
}
