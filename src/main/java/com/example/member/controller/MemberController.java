package com.example.member.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.dto.ArtistResultDTO;
import com.example.common.annotation.LoginUser;
import com.example.member.dto.MemberInfoResponseDTO;
import com.example.member.dto.MemberSignupRequest;
import com.example.member.dto.MemberUpdateRequestDTO;
import com.example.member.dto.PasswordUpdateRequestDTO;
import com.example.member.dto.RedisMemberDTO;
import com.example.member.service.MailSenderService;
import com.example.member.service.MemberService;
import com.example.security.tokenProvider.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/member")
@RequiredArgsConstructor
@RestController
@Slf4j
public class MemberController {
	
	private final MemberService memberService;
	private final MailSenderService mailSenderService;
	private final JwtTokenProvider jwtTokenProvider;
	
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
	public ResponseEntity<?> signup(@RequestBody MemberSignupRequest dto, HttpServletResponse response) {
		Map<String, Object> result = memberService.registerMember(dto, response);
		return ResponseEntity.ok(result);
	}
	
	// 로그인
	@PostMapping(value = "/login", produces = "application/json; charset=UTF-8")
	public ResponseEntity<?> login(@RequestBody Map<String, String> loginData, HttpServletResponse response) {
		try {
			Map<String, Object> result = memberService.login(
				loginData.get("email"), 
				loginData.get("password"),
				response
			);

			log.info("Controller 전송 직전 데이터 check - name: {}", result.get("name"));
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
	}
	
	// 로그아웃
	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
		String token = jwtTokenProvider.resolveToken(request);
		if(token != null) {
			memberService.logout(token, response);
			return ResponseEntity.ok(Map.of("message", "로그아웃 성공!"));
		}
		return ResponseEntity.badRequest().body(Map.of("message", "토큰이 없습니다."));
	}
	
	// 리프레시 토큰
	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(@CookieValue(name = "refreshToken") String refreshToken,
			HttpServletResponse response) {
		if(refreshToken == null) return ResponseEntity.status(401).body(Map.of("message", "다시 로그인해주세요."));
		try {
	        // 서비스에서 재발급 로직 수행
	        Map<String, Object> result = memberService.refreshToken(refreshToken, response);
	        return ResponseEntity.ok(result);
	    } catch (IllegalArgumentException e) {
	        return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
	    }
	}
	
	}
	
	// 개인정보 조회
	@GetMapping("/my-info")
	public ResponseEntity<MemberInfoResponseDTO> getMyInfo(@LoginUser RedisMemberDTO user) {
		if (user == null) {
			return ResponseEntity.status(401).build();
		}
		MemberInfoResponseDTO info = memberService.getMyInfo(user.getMemberId());
		return ResponseEntity.ok(info);
	}

	// 개인정보 수정
	@PostMapping("/update")
	public ResponseEntity<?> updateMemberInfo(@LoginUser RedisMemberDTO user, @RequestBody MemberUpdateRequestDTO dto) {
		if (user == null) {
			return ResponseEntity.status(401).build();
		}
		try {
			memberService.updateMemberInfo(user.getMemberId(), dto);
			return ResponseEntity.ok(Map.of("message", "정보가 성공적으로 수정되었습니다."));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
	}

	// 비밀번호 변경
	@PostMapping("/password")
	public ResponseEntity<?> updatePassword(@LoginUser RedisMemberDTO user, @RequestBody PasswordUpdateRequestDTO dto) {
		if (user == null) {
			return ResponseEntity.status(401).build();
		}
		try {
			memberService.updatePassword(user.getMemberId(), dto);
			return ResponseEntity.ok(Map.of("message", "비밀번호가 성공적으로 변경되었습니다."));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
	}
	
}
