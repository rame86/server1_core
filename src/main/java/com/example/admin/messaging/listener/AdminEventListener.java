package com.example.admin.messaging.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.admin.dto.EventResultDTO;
import com.example.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminEventListener {
	
	@RabbitListener(queues = RabbitMQConfig.EVENT_RES_QUEUE_NAME)
	public void receiveEventResult(EventResultDTO result) {
		log.info("=====> [1서버] 2서버가 보낸 이벤트 결과 도착!");
		
		if("CONFIRMED".equals(result.getStatus())) {
			
		}
	}

}
