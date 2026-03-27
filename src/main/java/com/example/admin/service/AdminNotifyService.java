package com.example.admin.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotifyService {
	
	private final SimpMessagingTemplate messagingTemplate;
	private static final String ADMIN_TOPIC = "/topic/notifications/admin";
	
	public void sendAlert(String message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("message", message);
        
        messagingTemplate.convertAndSend(ADMIN_TOPIC, payload);
        log.info("🔔 [관리자 알람] : {}", message);
    }

}
