package com.example.member.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailSenderService {
	
	private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
	
	// 메일 발송 기능
	public void sendEmail(String toEmail, String title, String content) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(toEmail);
		message.setSubject(title);
		message.setText(content);
		message.setFrom("gor_ge0us@naver.com");
		mailSender.send(message);
	}
	
	// 인증 번호 생성 및 발송 Redis 저장
	public void sendVerificationCode(String email) {
		
		// 1. 6자리 랜덤 인증번호 생성 (100000 ~ 999999)
    	String verificationCode = String.valueOf((int)(Math.random() * 899999) + 100000);
    	
    	// 2. Redis에 저장하기(key : email, Value : 인증번호, 유효시간 : 5분)
    	redisTemplate.opsForValue().set("CHECK:" + email, verificationCode, Duration.ofMinutes(5));
    	log.info("-----> [Redis 저장] 이메일: {}, 인증번호: {}", email, verificationCode);
    	
    	// 3. 메일 발송
        String title = "[TEST] 회원가입 인증번호입니다.";
        String content = "안녕하세요! 인증번호는 [" + verificationCode + "] 입니다. \n5분 이내에 입력해 주세요.";
        
        try {
        	sendEmail(email, title, content);
        } catch(Exception e) {
        	log.error("-----> [메일 발송 실패] 대상: {}, 사유: {}", email, e.getMessage());
        	throw new RuntimeException("메일 발송 중 오류가 발생했습니다.");
        } 
		
	}
	
	// 인증 번호 검증
	public boolean verifyCode(String email, String userInputCode) {
		String savedCode = redisTemplate.opsForValue().get("CHECK:" + email);
		if(savedCode!= null && savedCode.equals(userInputCode)) {
			return true;
		}
		return false;
	}
	
	// Redis 키 삭제(회원가입 완류 시 호출)
	public void deleteVerificationCode(String email) {
		redisTemplate.delete("CHECK:" + email);
	}

}
