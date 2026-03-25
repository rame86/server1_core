package com.example.admin.messaging.listener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.example.admin.dto.EventResultDTO;
import com.example.admin.dto.ShopResultDTO;
import com.example.admin.dto.UserListResponseDTO;
import com.example.admin.dto.UserPaymentSummaryDTO;
import com.example.admin.entity.Approval;
import com.example.admin.repository.ApprovalRepository;
import com.example.config.RabbitMQConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminEventListener {
    
    private final RabbitTemplate rabbitTemplate;
    private final ApprovalRepository approvalRepositroy; // 오타 주의 (approvalRepository)
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    
    @RabbitListener(queues = RabbitMQConfig.EVENT_REQ_QUEUE_NAME)
    public void handleEventResult(EventResultDTO dto) {
        log.info("=====> [1서버] 2서버로부터 EVENT 신청서 도착: {}", dto);
        saveToApproval(dto, "EVENT", dto.getApprovalId(), dto.getEventTitle(), dto.getRequesterId(), dto.getImageUrl());
    }
    
    @RabbitListener(queues = RabbitMQConfig.SHOP_REQ_QUEUE_NAME)
    public void handleShopRequest(ShopResultDTO dto) {
        log.info("=====> [1서버] 2서버로부터 SHOP 신청서 도착: {}", dto);
        saveToApproval(dto, "SHOP", dto.getApprovalId(), dto.getGoodsType(), dto.getRequesterId(), dto.getImageUrl());
    }
    
    private void saveToApproval(Object dto, String category, Long targetId, String title, Long requesterId, String imageUrl) {
        try {
            String jsonContent = objectMapper.writeValueAsString(dto);
            
            // Java 17 Pattern Matching 적용으로 불필요한 캐스팅 제거
            EventResultDTO eventDto = (dto instanceof EventResultDTO e) ? e : null;
            
            LocalDateTime startDate = (eventDto != null && eventDto.getEventStartDate() != null) 
                    ? LocalDateTime.parse(eventDto.getEventStartDate()) : null;

            Approval approval = Approval.builder()
                    .category(category)
                    .targetId(targetId)
                    .title(title)
                    .status("PENDING")
                    .contentJson(jsonContent)
                    .artistId(requesterId)
                    .eventStartDate(startDate)
                    .location(eventDto != null ? eventDto.getLocation() : null)
                    .price(eventDto != null ? eventDto.getPrice() : null)
                    .imageUrl(imageUrl)
                    .build();
                            
            approvalRepositroy.save(approval);
            log.info("=====> [1서버 DB] {} 신청 데이터 저장 완료!", category);
            
            sendAdminNotification(title, requesterId, category);
        } catch(Exception e) {
            log.error("=====> [1서버] {} 저장 중 오류 발생: {}", category, e.getMessage());
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

	@RabbitListener(queues = RabbitMQConfig.PAY_RES_QUEUE_NAME)
	public void handleUserPaymentSummary(List<UserPaymentSummaryDTO> responseList) {
		log.info("=====> [1서버] 결제 정보 응답 도착: {}건", responseList.size());
		try {
			messagingTemplate.convertAndSend("/topic/user-stats", responseList);
			log.info("=====> [WebSocket] 관리자 화면으로 실시간 결제 데이터 전송 완료");
		} catch (Exception e) {
			log.error("=====> [1서버] WebSocket 전송 중 오류: {}", e.getMessage());
		}
	}
	
	@RabbitListener(queues = RabbitMQConfig.ADMIN_PAY_RES_QUEUE_NAME)
	public void handleAdminPayResponse(Map<String, Object> response) {
	    log.info("📢 [1서버] 2서버로부터 응답 수신: {}", response);
	    
	    if (response == null || response.isEmpty()) return;

	    try {
	        // 1. 어떤 종류의 응답인지 확인 (보통 요청 보낼 때 넣은 orderId가 돌아옴)
	        String orderId = (String) response.get("orderId"); 
	        Object rawData = response.getOrDefault("payload", response.get("data"));
	        
	        if (rawData == null) return;

	        // 2. [SUMMARY] 타입 응답 처리 (구매건수 + 잔액 들어있음)
	        if ("SUMMARY".equals(orderId)) {
	            List<UserPaymentSummaryDTO> summaryList = objectMapper.convertValue(
	                rawData, new TypeReference<List<UserPaymentSummaryDTO>>() {}
	            );
	            
	            // 핵심: SUMMARY 데이터를 WebSocket으로 전송!
	            // 프론트엔드에서 이 데이터를 받을 수 있도록 주소를 맞춥니다.
	            messagingTemplate.convertAndSend("/topic/user-stats", summaryList);
	            log.info("🚀 [WebSocket] SUMMARY 데이터 전송 완료 ({}건)", summaryList.size());
	        } 
	        
	        // 3. [GETALL] 타입 응답 처리 (기존 로직 유지)
	        else if ("GETALL".equals(orderId)) {
	            List<UserListResponseDTO> userList = objectMapper.convertValue(
	                rawData, new TypeReference<List<UserListResponseDTO>>() {}
	            );
	            messagingTemplate.convertAndSend("/topic/user-stats", userList);
	            log.info("🚀 [WebSocket] GETALL 데이터 전송 완료");
	        }

	    } catch (Exception e) {
	        log.error("❌ [1서버] 데이터 처리 중 오류 발생: {}", e.getMessage());
	    }
	}
    
}
