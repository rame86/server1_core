package com.example.admin.messaging.listener;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.example.admin.dto.EventResultDTO;
import com.example.admin.dto.ShopResultDTO;
import com.example.admin.entity.Approval;
import com.example.admin.repository.ApprovalRepository;
import com.example.config.RabbitMQConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminEventListener {
	
	private final RabbitTemplate rabbitTemplate;
	private final ApprovalRepository approvalRepositroy;
	private final ObjectMapper objectMapper; // JSON 변환기
	
	@RabbitListener(queues = RabbitMQConfig.EVENT_REQ_QUEUE_NAME)
	public void handleEventResult(EventResultDTO dto) {
		log.info("=====> [1서버] 2서버로부터 신청서 도착: {}", dto);
		saveToApproval(dto, "EVENT", dto.getRequesterId(), dto.getEventTitle(), dto.getRequesterId());
	}
	
	@RabbitListener(queues = RabbitMQConfig.SHOP_REQ_QUEUE_NAME)
	public void handleShopRequest(ShopResultDTO dto) {
		log.info("=====> [1서버] 2서버로부터 신청서 도착: {}", dto);
		
		String adminMessage = "새로운 굿즈 [" + dto.getGoodsType() + "] 신청이 들어왔습니다. 검토해주세요!";
		
		Map<String, String> message = new HashMap<>();
		message.put("type", "SHOP_NEW_REQUEST");
		message.put("requesterId", String.valueOf(dto.getRequesterId()));
		message.put("message", adminMessage);
		message.put("createdAt", dto.getCreatedAt());
		
		rabbitTemplate.convertAndSend("amq.topic", "notification.admin", message);
	}
	
	private void saveToApproval(Object dto, String category, Long targetId, String title, Long requesterId) {
		try {
			// DTO 객체를 JSON 문자열로 변환
			String jsonContent = objectMapper.writeValueAsString(dto);
			
			Approval approval = Approval.builder()
					.category(category)
					.targetId(targetId)
					.title(title)
					.status("PENDING")
					.contentJson(jsonContent)
					.artistId(requesterId)
					.build();
			
			approvalRepositroy.save(approval);
			log.info("=====> [1서버 DB] {} 신청 데이터 저장 완료!", category);
			
			sendAdminNotification(title, requesterId, category);
		} catch(Exception e) {
			log.error("=====> [1서버] 저장 중 오류 발생: {}", e.getMessage());
		}
	}
	
	private void sendAdminNotification(String title, Long requesterId, String category) {
		String typeName = category.equals("EVENT") ? "SHOPT" : "PAY";
		String adminMessage = "새로운 " + typeName + " [" + title + "] 신청이 들어왔습니다.";
		
		Map<String, String> message = new HashMap<>();
		message.put("type", "NEW_REQUEST");
        message.put("category", category);
        message.put("message", adminMessage);
        
        rabbitTemplate.convertAndSend("amq.topic", "notification.admin", message);
	}
	
}
