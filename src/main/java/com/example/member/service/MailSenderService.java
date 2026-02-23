package com.example.member.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailSenderService {
	
	private final JavaMailSender mailSender;
	
	public void sendEmail(String toEmail, String title, String content) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(toEmail);
		message.setSubject(title);
		message.setText(content);
		message.setFrom("gor_ge0us@naver.com");
		mailSender.send(message);
	}

}
