package com.example.admin.messaging.listener;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.example.admin.dto.EventResultDTO;
import com.example.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminEventListener {
	
	private final RabbitTemplate rabbitTemplate;
	
	@RabbitListener(queues = RabbitMQConfig.EVENT_REQ_QUEUE_NAME)
	public void handleEventResult(EventResultDTO dto) {
		log.info("=====> [1서버] 2서버로부터 신청서 도착: {}", dto);
		
		// 알림 메시지 내용 구성
		String adminMessage = "새로운 공연 [" + dto.getEventTitle() + "] 신청이 들어왔습니다. 검토해주세요!";
		
		Map<String, String> message = new HashMap<>();
		message.put("type", "NEW_REQUEST");
		message.put("requesterId", String.valueOf(dto.getRequesterId()));
		message.put("message", adminMessage);
		message.put("createdAt", dto.getCreatedAt());
		
		// 프론트로 던지기
		rabbitTemplate.convertAndSend("amq.topic", "notification.admin", message);
		
	}
	
}
