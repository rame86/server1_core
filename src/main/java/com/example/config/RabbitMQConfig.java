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
	
	public static final String EXCHANGE_NAME = "msa.direct.exchange";
	
	// 결제관련
	public static final String PAY_REQ_ROUTING_KEY = "pay.request";
	public static final String PAY_RES_ROUTING_KEY = "pay.res.core";
    public static final String PAY_RES_QUEUE_NAME = "pay.res.core.queue";
    
    // 이벤트관련
    public static final String EVENT_REQ_ROUTING_KEY= "admin.event.request";
    public static final String EVENT_REQ_QUEUE_NAME = "admin.event.request.queue";
    public static final String EVENT_RES_ROUTING_KEY="event.res.core";
    public static final String EVENT_RES_QUEUE_NAME="event.res.core.queue";
    
    // 굿즈관련
    public static final String SHOP_REQ_ROUTING_KEY= "shop.request";
    public static final String SHOP_REQ_QUEUE_NAME = "shop.request.queue";
    public static final String SHOP_RES_ROUTING_KEY="shop.res.core";
    public static final String SHOP_RES_QUEUE_NAME="shop.res.core.queue";

    // 게시판(Board) 관련 추가
    public static final String BOARD_REQ_ROUTING_KEY = "admin.board.request";
    public static final String BOARD_REQ_QUEUE_NAME = "admin.board.request.queue";
    public static final String BOARD_RES_ROUTING_KEY = "board.res.core";
    public static final String BOARD_RES_QUEUE_NAME = "board.res.core.queue";

    // [추가] 게시판 신고 승인 관련
    public static final String BOARD_REPORT_APPROVE_QUEUE_NAME = "board.report.approve.queue";
    public static final String BOARD_REPORT_APPROVE_ROUTING_KEY = "board.report.approve.key";
    
    // 환불 요청 관련
    public static final String REFUND_REQ_ROUTING_KEY = "refund.req.core";
    public static final String REFUND_REQ_QUEUE_NAME = "refund.req.core.queue";
    public static final String REFUND_RES_ROUTING_KEY = "refund.res.core";
    public static final String REFUND_RES_QUEUE_NAME = "refund.res.core.queue";
    
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
    public Queue eventRequestQueue() {
        return new Queue(EVENT_REQ_QUEUE_NAME, true);
    }
    
    @Bean
    public Binding eventRequestBinding(@Qualifier("eventRequestQueue") Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(EVENT_REQ_ROUTING_KEY);
    }
    
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
    public Queue shopRequestQueue() {
        return new Queue(SHOP_REQ_QUEUE_NAME, true);
    }
    
    @Bean
    public Binding shopRequestBinding(@Qualifier("shopRequestQueue") Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(SHOP_REQ_ROUTING_KEY);
    }
    
    @Bean
    public Queue shopReplyQueue() {
    	return new Queue(SHOP_RES_QUEUE_NAME, true);
    }
    
    @Bean
    public Binding shopReplyBinding(@Qualifier("shopReplyQueue") Queue queue, DirectExchange exchange) {
    	return BindingBuilder.bind(queue).to(exchange).with(SHOP_RES_ROUTING_KEY);
    }

    // 게시판(Board) 관련 Bean 추가
    @Bean
    public Queue boardRequestQueue() {
        return new Queue(BOARD_REQ_QUEUE_NAME, true);
    }

    // [추가] 게시판 신고 승인 관련 Bean
    @Bean
   public Queue boardReportApproveQueue() {
        return new Queue(BOARD_REPORT_APPROVE_QUEUE_NAME, true);
    }
    // [추가] 신고 승인 큐와 익스체인지를 연결하는 바인딩 (이게 있어야 메시지가 전달됩니다)
    @Bean
    public Binding boardReportApproveBinding(@Qualifier("boardReportApproveQueue") Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(BOARD_REPORT_APPROVE_ROUTING_KEY);
    }
    
    @Bean
    public Binding boardRequestBinding(@Qualifier("boardRequestQueue") Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(BOARD_REQ_ROUTING_KEY);
    }

    @Bean
    public Queue boardReplyQueue() {
        return new Queue(BOARD_RES_QUEUE_NAME, true);
    }

    @Bean
    public Binding boardReplyBinding(@Qualifier("boardReplyQueue") Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(BOARD_RES_ROUTING_KEY);
    }
    
    // 환불 요청
    @Bean
    public Queue refundRequestQueue() {
        return new Queue(REFUND_REQ_QUEUE_NAME, true);
    }

    @Bean
    public Binding refundRequestBinding(@Qualifier("refundRequestQueue") Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(REFUND_REQ_ROUTING_KEY);
    }
    
    @Bean
    public Queue refundReplyQueue() {
        return new Queue(REFUND_RES_QUEUE_NAME, true);
    }

    @Bean
    public Binding refundReplyBinding(@Qualifier("refundReplyQueue") Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(REFUND_RES_ROUTING_KEY);
    }
 
    // 메시지 발행 시 객체를 JSON 포맷으로 변환
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
}