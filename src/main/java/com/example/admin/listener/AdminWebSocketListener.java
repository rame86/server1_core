package com.example.admin.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.example.admin.dto.user.UserListResponseDTO;
import com.example.admin.dto.user.UserPaymentSummaryDTO;
import com.example.config.RabbitMQConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminWebSocketListener {
	
	private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    
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
	        
	        // 웸소켓으로 보낼 공통 응답 객ㅊ에 생성
	        Map<String, Object> socketData = new HashMap<>();

	        // 2. [SUMMARY] 타입 응답 처리 (구매건수 + 잔액 들어있음)
	        if ("SUMMARY".equals(orderId)) {
	            List<UserPaymentSummaryDTO> summaryList = objectMapper.convertValue(
	                rawData, new TypeReference<List<UserPaymentSummaryDTO>>() {}
	            );
	            
	            // 핵심: type달기
	            socketData.put("type", "SUMMARY");
	            socketData.put("payload", summaryList);
	            
	            // 프론트엔드에서 이 데이터를 받을 수 있도록 주소를 맞춥니다.
	            messagingTemplate.convertAndSend("/topic/user-stats", socketData);
	            log.info("🚀 [WebSocket] SUMMARY 데이터 전송 완료 ({}건)", summaryList.size());
	        } 
	        
	        // 3. [GETALL] 타입 응답 처리 (기존 로직 유지)
	        else if ("GETALL".equals(orderId)) {
	            List<UserListResponseDTO> userList = objectMapper.convertValue(
	                rawData, new TypeReference<List<UserListResponseDTO>>() {}
	            );
	            
	            // 핵심: 이름표(type) 달기
	            socketData.put("type", "GETALL");
	            socketData.put("payload", userList);
	            
	            messagingTemplate.convertAndSend("/topic/user-stats", socketData);
	            log.info("🚀 [WebSocket] GETALL 데이터 전송 완료");
	        } else if ("USER_DETAIL".equals(orderId)) {
	        	socketData.put("type", "USER_DETAIL");
	        	socketData.put("payload", rawData);
	        	
	        	messagingTemplate.convertAndSend("/topic/user-stats", socketData);
	        	log.info("🚀 [WebSocket] USER_DETAIL 데이터 전송 완료! (ID: {})", response.get("memberId"));
	        }

	    } catch (Exception e) {
	        log.error("❌ [1서버] 데이터 처리 중 오류 발생: {}", e.getMessage());
	    }
	}

}
