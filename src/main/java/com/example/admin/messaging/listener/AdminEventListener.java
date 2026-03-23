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

    @RabbitListener(queues = RabbitMQConfig.ADMIN_PAY_RES_QUEUE_NAME)
    public void handleUserPaymentSummary(Map<String, Object> response) {
        log.info("=====> [1서버] 결제 정보 응답 도착");
        
        // 1. 방어적 로직: 빈 메시지 예외 처리
        if (response == null || response.isEmpty()) {
            log.warn("=====> [1서버] 빈 결제 데이터 응답이 수신되었습니다.");
            return;
        }

        try {
            Object rawData = response.getOrDefault("payload", response.get("data"));
            
            if (rawData != null) {
                // 2. 파싱 예외를 구체적으로 캐치하여 데이터 포맷 불일치 오류 추적 용이
                List<UserPaymentSummaryDTO> list = objectMapper.convertValue(
                    rawData, 
                    new TypeReference<List<UserPaymentSummaryDTO>>() {}
                );
                
                messagingTemplate.convertAndSend("/topic/user-stats", list);
                log.info("=====> [WebSocket] 관리자 화면으로 실시간 결제 데이터 전송 완료 ({}건)", list.size());
            } else {
                log.warn("=====> [1서버] 수신된 응답에 유저 데이터(payload)가 없음. 원본: {}", response);
            }
        } catch (IllegalArgumentException e) {
            log.error("=====> [1서버] 결제 데이터 DTO 매핑 오류 (필드 타입 불일치 등): {}", e.getMessage());
        } catch (Exception e) {
            log.error("=====> [1서버] WebSocket 전송 중 오류: {}", e.getMessage());
        }
    }
}
