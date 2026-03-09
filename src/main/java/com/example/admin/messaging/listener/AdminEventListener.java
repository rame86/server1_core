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
	
	@RabbitListener(queues = RabbitMQConfig.EVENT_RES_QUEUE_NAME)
	public void handleEventResult(EventResultDTO dto) {
		log.info("=====> [1서버] 2서버로부터 결과 도착: {}", dto);
		
		// 알림 메시지 내용 구성
		String statusMessage = "CONFIRMED".equals(dto.getStatus())
				? "축하합니다! [" + dto.getEventTitle() + "] 공연이 최종 승인되었습니다. 🎉"
						: "[" + dto.getEventTitle() + "] 공연이 반려되었습니다. 사유: " + dto.getRejectionReason();
		
		// message에 담기
		Map<String, String> message = new HashMap<>();
		message.put("type", "EVENT_NOTIFICATION");
		message.put("requesterId", String.valueOf(dto.getRequesterId()));
		message.put("message", statusMessage);
		message.put("createdAt", dto.getCreatedAt());
		
		// 프론트로 던지기
		rabbitTemplate.convertAndSend("amq.topic", "notification.artist", message);
		
		log.info("=====> [1서버] 아티스트(ID:{})에게 실시간 알림 전송 완료!", dto.getRequesterId());
	}
	
}
