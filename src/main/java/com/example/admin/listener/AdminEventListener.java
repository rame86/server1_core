package com.example.admin.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.admin.dto.approval.ShopResultDTO;
import com.example.admin.dto.event.EventResultDTO;
import com.example.admin.service.AdminApprovalService;
import com.example.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminEventListener {
    
    private final AdminApprovalService adminApprovalService;
    
    @RabbitListener(queues = RabbitMQConfig.EVENT_REQ_QUEUE_NAME)
    public void handleEventResult(EventResultDTO dto) {
        log.info("=====> [1서버] EVENT 신청서 도착: {}", dto);
        adminApprovalService.saveEventApproval(dto);
    }
    
    @RabbitListener(queues = RabbitMQConfig.SHOP_REQ_QUEUE_NAME)
    public void handleShopRequest(ShopResultDTO dto) {
        log.info("=====> [1서버] SHOP 신청서 도착: {}", dto);
        adminApprovalService.saveShopApproval(dto);
    }
    
}
