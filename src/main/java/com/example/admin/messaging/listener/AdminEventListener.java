package com.example.admin.messaging.listener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.example.admin.dto.EventResultDTO;
import com.example.admin.dto.ShopResultDTO;
import com.example.admin.dto.UserListResponseDTO;
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
		saveToApproval(dto, "EVENT", dto.getApprovalId(), dto.getEventTitle(), dto.getRequesterId(), dto.getImageUrl());
	}
	
	@RabbitListener(queues = RabbitMQConfig.SHOP_REQ_QUEUE_NAME)
	public void handleShopRequest(ShopResultDTO dto) {
		log.info("=====> [1서버] 2서버로부터 신청서 도착: {}", dto);
		saveToApproval(dto, "SHOP", dto.getApprovalId(), dto.getGoodsType(), dto.getRequesterId(), dto.getImageUrl());
	}
	
	private void saveToApproval(Object dto, String category, Long targetId, String title, Long requesterId, String imageUrl) {
		try {
			// DTO 객체를 JSON 문자열로 변환
			String jsonContent = objectMapper.writeValueAsString(dto);
			EventResultDTO eventDto = (dto instanceof EventResultDTO) ? (EventResultDTO) dto : null;
			
			Approval approval = Approval.builder()
					.category(category)
					.targetId(targetId)
					.title(title)
					.status("PENDING")
					.contentJson(jsonContent)
					.artistId(requesterId)
					.eventStartDate(eventDto != null && eventDto.getEventStartDate() != null 
		                    ? LocalDateTime.parse(eventDto.getEventStartDate()) : null)
		            .location(eventDto != null ? eventDto.getLocation() : null)
		            .price(eventDto != null ? eventDto.getPrice() : null)
		            .imageUrl(imageUrl)
		            .build();
							
			approvalRepositroy.save(approval);
			log.info("=====> [1서버 DB] {} 신청 데이터 저장 완료!", category);
			
			sendAdminNotification(title, requesterId, category);
		} catch(Exception e) {
			log.error("=====> [1서버] 저장 중 오류 발생: {}", e.getMessage());
		}
	}
	
	private void sendAdminNotification(String title, Long requesterId, String category) {
		String typeName = category.equals("EVENT") ? "EVENT" : "SHOP";
		String adminMessage = "새로운 " + typeName + " [" + title + "] 신청이 들어왔습니다.";
		
		Map<String, String> message = new HashMap<>();
		message.put("type", "NEW_REQUEST");
        message.put("category", category);
        message.put("message", adminMessage);
        
        rabbitTemplate.convertAndSend("amq.topic", "notification.admin", message);
	}
	
	@RabbitListener(queues = RabbitMQConfig.ADMIN_PAY_RES_QUEUE_NAME) // 2서버가 답장 쏘는 큐!
	public void handleUserPaymentResponse(List<UserListResponseDTO> responseList) {
	    log.info("📢 [1서버 비동기 리스너] 2서버에서 보낸 결제 데이터 도착! 건수: {}", responseList.size());

	    try {
	        for (UserListResponseDTO dto : responseList) {
	            // 🚔 비동기식의 핵심: 받은 데이터를 DB에 업데이트해야 나중에 화면에 나와!
	            // 예: memberRepository.updatePaymentInfo(dto.getMemberId(), dto.getPointBalance(), dto.getPurchaseCount());
	            log.info("유저 {}님의 잔액 {}원 업데이트 완료!", dto.getMemberId(), dto.getPointBalance());
	        }
	    } catch (Exception e) {
	        log.error("❌ [1서버] 비동기 데이터 처리 중 오류: {}", e.getMessage());
	    }
	}
	
}
