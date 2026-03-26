package com.example.admin.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.dto.approval.ApprovalDTO;
import com.example.admin.dto.approval.ShopResultDTO;
import com.example.admin.dto.event.EventResultDTO;
import com.example.admin.entity.Approval;
import com.example.admin.repository.ApprovalRepository;
import com.example.config.RabbitMQConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminApprovalService {

	private final RabbitTemplate rabbitTemplate;
	private final ApprovalRepository approvalRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public void processApproval(ApprovalDTO dto, String routingKey, Long adminId) {
		Long eventId = updateApprovalStatus(dto, adminId);
		if (dto instanceof EventResultDTO eventResultDto) {
			eventResultDto.setApprovalId(eventId);
		}
		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, dto);
	}

	public Long updateApprovalStatus(ApprovalDTO dto, Long adminId) {
		Approval approval = findApproval(dto.getApprovalId());
		approval.setStatus(dto.getStatus());
		approval.setAdminId(adminId);
		approval.setProcessedAt(LocalDateTime.now());

		if ("REJECTED".equals(dto.getStatus())) {
			approval.setRejectionReason(dto.getRejectionReason());
		}

		return approval.getTargetId();
	}

	public Approval findApproval(Long approvalId) {
		return approvalRepository.findById(approvalId)
				.orElseThrow(() -> new IllegalArgumentException("해당 신청서(ID: " + approvalId + ")가 존재하지 않습니다."));
	}
	
	// 리스너로부터 호출되는 로직
	@Transactional
	public void saveEventApproval(EventResultDTO dto) {
        Approval approval = Approval.builder()
                .category("EVENT")
                .targetId(dto.getApprovalId())
                .title(dto.getEventTitle())
                .artistId(dto.getRequesterId())
                .status("PENDING")
                .contentJson(toJson(dto))
                .eventStartDate(parseDate(dto.getEventStartDate()))
                .eventEndDate(parseDate(dto.getEventEndDate()))
                .eventDate(parseDate(dto.getEventDate()))
                .location(dto.getLocation())
                .price(dto.getPrice())
                .imageUrl(dto.getImageUrl())
                .build();

        saveAndNotify(approval, dto.getEventTitle(), dto.getRequesterId(), "EVENT");
    }
	
	// 리스너로부터 호출되는 로직
	@Transactional
    public void saveShopApproval(ShopResultDTO dto) {
        Approval approval = Approval.builder()
                .category("SHOP")
                .targetId(dto.getApprovalId())
                .title(dto.getGoodsName())
                .artistId(dto.getRequesterId())
                .status("PENDING")
                .contentJson(toJson(dto))
                .price(dto.getPrice() != null ? dto.getPrice().longValue() : null)
                .imageUrl(dto.getImageUrl())
                .build();

        saveAndNotify(approval, dto.getGoodsName(), dto.getRequesterId(), "SHOP");
    }
	
	private void saveAndNotify(Approval approval, String title, Long requesterId, String category) {
        try {
            approvalRepository.save(approval);
            log.info("=====> [1서버 DB] {} 저장 완료", category);
            sendAdminNotification(title, requesterId, category);
        } catch (Exception e) {
            log.error("=====> 저장 실패: {}", e.getMessage());
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
	
	private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
	
    private LocalDateTime parseDate(String dateStr) {
        return (dateStr != null && !dateStr.isBlank()) ? LocalDateTime.parse(dateStr) : null;
    }
    
}
