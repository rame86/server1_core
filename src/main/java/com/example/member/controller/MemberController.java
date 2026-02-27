package com.example.member.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.member.dto.MemberSignupRequest;
import com.example.member.service.MailSenderService;
import com.example.member.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/member")
@RequiredArgsConstructor
@RestController
@Slf4j
public class MemberController {
	
	private final MemberService memberService;
	private final MailSenderService mailSenderService;
	
	// 인증 메일 발송
	@PostMapping("/SendVerification")
	public ResponseEntity<?> sendVerificationCode(@RequestParam("email") String email) {
		try {
			mailSenderService.sendVerificationCode(email);
			return ResponseEntity.ok(Map.of("message", "인증번호가 발송되었습니다."));
		} catch (RuntimeException e) {
			return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
		}
	}
	
	// 회원 가입
	@PostMapping("/signup")
	public ResponseEntity<?> signup(@RequestBody MemberSignupRequest dto) {
		Map<String, Object> result = memberService.registerMember(dto);
		return ResponseEntity.ok(result);
	}
	
	// 로그인
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
		try {
			Map<String, Object> result = memberService.login(
				loginData.get("email"), 
				loginData.get("password")
			);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
	}

}
