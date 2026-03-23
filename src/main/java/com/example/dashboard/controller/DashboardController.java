package com.example.dashboard.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.config.RabbitMQConfig;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final RabbitTemplate rabbitTemplate;

    public DashboardController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/dashboard-queue")
    public ResponseEntity<String> triggerDashboardQueue(@RequestBody Map<String, Object> request) {
        return handleQueueSignal(request, "USER");
    }

    private ResponseEntity<String> handleQueueSignal(Map<String, Object> request, String role) {
        try {
            String memberId = String.valueOf(request.get("memberId"));
            
            // 전송할 메시지 생성
            Map<String, Object> message = new HashMap<>();
            message.put("memberId", memberId);
            message.put("action", "FETCH_DASHBOARD");
            message.put("role", role);

            // 각 서비스로 큐 발송
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PAY_REQ_ROUTING_KEY, message);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.EVENT_REQ_ROUTING_KEY, message);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.SHOP_REQ_ROUTING_KEY, message);
            
            return ResponseEntity.ok("Dashboard fetch signal sent to queues successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to send dashboard fetch signal: " + e.getMessage());
        }
    }
}
