package com.example.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
	
	public static final String EXCHANGE_NAME = "msa.direct.exchange";
	
	public static final String PAY_ROUTING_KEY = "pay.request";
	
	public static final String ARTIST_ROUTING_KEY = "artist.res.pay";
    public static final String ARTIST_RESPONSE_QUEUE_NAME = "artist.res.pay.queue";
    
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }
    
    @Bean
    public Queue replyQueue() {
        return new Queue(ARTIST_RESPONSE_QUEUE_NAME, true);
    }
    
    @Bean
    public Binding replyBinding(Queue replyQueue, DirectExchange exchange) {
    	return BindingBuilder.bind(replyQueue).to(exchange).with(ARTIST_ROUTING_KEY);
    }
 
    // 메시지 발행 시 객체를 JSON 포맷으로 변환
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
}