package com.example.dashboard.message;

import com.example.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardMessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.DASHBOARD_RES_QUEUE_NAME)
    public void handleDashboardResponse(Map<String, Object> response) {
        log.info("=====> [Core 서버] 대시보드 데이터 응답 도착: {}", response);
        try {
            String memberId = String.valueOf(response.get("memberId"));
            if (memberId != null && !memberId.equals("null")) {
                messagingTemplate.convertAndSend("/topic/dashboard/" + memberId, response);
                log.info("=====> [WebSocket] 사용자 {} 화면으로 대시보드 실시간 데이터 전송 완료", memberId);
            } else {
                log.warn("=====> [Core 서버] 수신된 응답에 memberId가 없음. 원본: {}", response);
            }
        } catch (Exception e) {
            log.error("=====> [Core 서버] WebSocket 전송 중 오류: {}", e.getMessage());
        }
    }
}
