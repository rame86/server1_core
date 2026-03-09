package com.example.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }
    
    // 결제 관련
    @Bean
    public Queue payReplyQueue() {
        return new Queue(PAY_RES_QUEUE_NAME, true);
    }
    
    @Bean
    public Binding payReplyBinding(@Qualifier("payReplyQueue") Queue queue, DirectExchange exchange) {
    	return BindingBuilder.bind(queue).to(exchange).with(PAY_RES_ROUTING_KEY);
    }
    
    // 이벤트 관련
    @Bean
    public Queue eventReplyQueue() {
    	return new Queue(EVENT_RES_QUEUE_NAME, true);
    }
    
    @Bean
    public Binding eventReplyBinding(@Qualifier("eventReplyQueue") Queue queue, DirectExchange exchange) {
    	return BindingBuilder.bind(queue).to(exchange).with(EVENT_RES_ROUTING_KEY);
    }
    
    // 굿즈 관련
    @Bean
    public Queue shopReplyQueue() {
    	return new Queue(SHOP_RES_QUEUE_NAME, true);
    }
    
    @Bean
    public Binding shopReplyBinding(@Qualifier("shopReplyQueue") Queue queue, DirectExchange exchange) {
    	return BindingBuilder.bind(queue).to(exchange).with(SHOP_RES_ROUTING_KEY);
    }
 
    // 메시지 발행 시 객체를 JSON 포맷으로 변환
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
 
    // 메시지 발행 시 객체를 JSON 포맷으로 변환
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
}