package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // 웹소켓 메시지 핸들링 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지를 받을 때 사용할 prefix 설정
        // /topic으로 시작하는 메시지는 브로커(SimpleBroker)가 구독자들에게 전달함
        config.enableSimpleBroker("/topic");
        
        // 메시지를 보낼 때 사용할 prefix 설정 (필요 시)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 프론트엔드에서 웹소켓에 접속할 엔드포인트 설정
        registry.addEndpoint("/ws-admin")
                .setAllowedOriginPatterns("*") // 테스트 시 모든 도메인 허용 (운영 시 제한 필요)
                .withSockJS(); // SockJS 지원 (브라우저 호환성)
    }
    
}
