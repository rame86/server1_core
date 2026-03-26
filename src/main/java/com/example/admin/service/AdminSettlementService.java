package com.example.admin.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.example.admin.dto.settlement.SettlementDashboardResponse;
import com.example.artist.dto.PaymentRequestDTO;
import com.example.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSettlementService {
	
	private final RabbitTemplate rabbitTemplate;
	private final Map<String, CompletableFuture<SettlementDashboardResponse>> pendingRequests;
	private static final int MQ_TIMEOUT_SECONDS = 1;
	
	public SettlementDashboardResponse requestDashboardData() {
		String requestId = "ADMIN_SETTLEMENT_REQ";
		CompletableFuture<SettlementDashboardResponse> future = new CompletableFuture<>();
		pendingRequests.put(requestId, future);

		PaymentRequestDTO dto = PaymentRequestDTO.builder()
				.type("ADMIN_SETTLEMENT")
				.replyRoutingKey(RabbitMQConfig.ADMIN_PAY_RES_ROUTING_KEY)
				.build();
		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PAY_REQ_ROUTING_KEY, dto);

		try {
			return future.get(MQ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (Exception e) {
			pendingRequests.remove(requestId);
			return new SettlementDashboardResponse(null, null);
		}
	}

}
