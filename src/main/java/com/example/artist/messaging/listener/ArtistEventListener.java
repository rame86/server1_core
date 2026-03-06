package com.example.artist.messaging.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.artist.dto.PaymentResponseDTO;
import com.example.artist.entity.Donation;
import com.example.artist.repository.DonationRepository;
import com.example.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArtistEventListener {
	
	private final DonationRepository donationRepository;

	// pay로부터 처리 결과를 수신
	@RabbitListener(queues = RabbitMQConfig.ARTIST_RESPONSE_QUEUE_NAME)
	@Transactional
	public void handlePaymentResult(PaymentResponseDTO response) {
		log.info("=====> [Artist 서버 수신] 주문번호: {}, 상태: {}, 메시지: {}", 
                response.getOrderId(), response.getStatus(), response.getMessage());
		
		// 주문번호로 DB에서 주문 정보 찾아오기
		Donation donation = donationRepository.findByOrderId(response.getOrderId())
				.orElseThrow(() -> new RuntimeException("해당 주문을 찾을 수 없습니다 : " + response.getOrderId()));
		
		// 응답 상타에 따라 DB의 status를 업데이트
		if("COMPLETE".equals(response.getStatus())) {
			donation.complete();
			log.info("----> 주문번호 {} 후원 처리 완료(DB업데이트 완료)", response.getOrderId());
		} else if("FAIL".equals(response.getStatus())){
			donation.fail();
			log.info("----> 주문번호 {} 후원 처리 실패(DB업데이트 완료){}", response.getOrderId(), response.getMessage());
		}
	}
	
}
