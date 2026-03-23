package com.example.dashboard.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.example.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 유저 대시보드 데이터 요청 서비스.
 * Pay 서비스에 MQ로 유저 상세 내역(포인트, 구매) 요청을 보냄.
 * 관리자의 AdminService.getAllUserList() 패턴을 유저용으로 구현.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDashboardService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Pay 서비스에 특정 유저의 대시보드 데이터를 요청.
     * type=ADMIN, orderId=USER_DETAIL 규격을 따름 (PaymentEventListener 참고).
     */
    public void requestUserDashboardData(Long memberId) {
        log.info("=====> [UserDashboardService] 유저 ID: {} 대시보드 데이터 요청 시작", memberId);

        Map<String, Object> message = new HashMap<>();
        message.put("type", "ADMIN");
        message.put("orderId", String.valueOf(memberId)); // Pay 응답의 orderId로 echo되어 돌아옴 → memberId로 사용
        message.put("memberId", memberId);
        // 응답 받을 전용 큐의 라우팅 키
        message.put("replyRoutingKey", RabbitMQConfig.USER_DASHBOARD_PAY_RES_ROUTING_KEY);

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.PAY_REQ_ROUTING_KEY,
            message
        );

        log.info("=====> [UserDashboardService] Pay 서비스로 MQ 요청 완료 (memberId: {})", memberId);
    }
}
