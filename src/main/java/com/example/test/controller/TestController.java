package com.example.test.controller;

/*
[Restful 방식]
	의미	 	http 메소드
	Create	POST(*)
	Read	GET(*)
	Update	PUT
	Delete	DELETE
	
	(*)표준

[ 기존 URL과 Restful 비교 ]

` 게시판목록보기	/board/getBoardList				/board			GET
` 게시글입력화면	/board/insertBoard				/board/write	GET
` 게시글입력(작성)	/board/saveBoard				/board/write	POST
` 게시글상세보기	/board/getBoard?seq=글번호		/board/글번호		GET
` 게시글수정		/board/updateBoard?seq=글번호		/board/글번호		PUT
` 게시글삭제		/board/deleteBoard?seq=글번호		/board/글번호		DELETE
*/

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final RabbitTemplate rabbitTemplate;
    
    // pay 서비스의 RabbitMQConfig와 동일한 Exchange 및 Routing Key 지정
    private static final String EXCHANGE_NAME = "msa.direct.exchange";
    private static final String ROUTING_KEY = "pay.request";

    public TestController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // 테스트용 DTO (pay 서비스의 PaymentRequestDto 필드와 매핑)
    public record PaymentRequestDto(String reservationId, Long amount) {}

    @PostMapping("/pay-request")
    public ResponseEntity<String> sendPaymentRequest(@RequestBody PaymentRequestDto requestDto) {
        try {
            // 지정된 Exchange로 Routing Key를 포함하여 메시지 발행
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, requestDto);
            return ResponseEntity.ok("결제 요청 메시지 큐 전송 성공: " + requestDto.reservationId());
        } catch (Exception e) {
            // RabbitMQ 브로커 연결 실패 등 예외 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("메시지 큐 전송 실패: " + e.getMessage());
        }
    }
}