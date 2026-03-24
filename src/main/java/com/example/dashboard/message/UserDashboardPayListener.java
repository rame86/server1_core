package com.example.dashboard.message;

import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.example.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 유저 대시보드 전용 Pay 응답 리스너.
 * AdminEventListener.handleUserPaymentSummary() 패턴을 유저용으로 구현.
 * Pay 서비스에서 USER_DETAIL 응답이 오면 해당 유저의 STOMP 채널로 전달.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDashboardPayListener {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.USER_DASHBOARD_PAY_RES_QUEUE_NAME)
    public void handleUserDashboardPayResponse(Map<String, Object> response) {
        log.info("=====> [UserDashboardPayListener] Pay 서비스 응답 도착: status={}", response.get("status"));
        try {
            // Pay 서비스 응답 구조: {orderId, status, message, type, payload}
            // orderId에 memberId가 echo되어 돌아옴 (UserDashboardService에서 orderId=memberId로 전송)
            Object payloadRaw = response.get("payload");
            String memberId = String.valueOf(response.get("orderId")); // orderId = memberId

            if (payloadRaw == null) {
                log.warn("=====> [UserDashboardPayListener] payload가 null임. 원본: {}", response);
                return;
            }

            if (memberId == null || memberId.equals("null")) {
                log.warn("=====> [UserDashboardPayListener] orderId(memberId)가 null임");
                return;
            }

            // STOMP를 통해 해당 유저 전용 토픽으로 전송
            messagingTemplate.convertAndSend("/topic/user-dashboard/" + memberId, payloadRaw);
            log.info("=====> [WebSocket] 유저 {} 대시보드로 결제 데이터 전송 완료", memberId);

        } catch (Exception e) {
            log.error("=====> [UserDashboardPayListener] WebSocket 전송 중 오류: {}", e.getMessage(), e);
        }
    }
}
