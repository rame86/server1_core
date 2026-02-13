package com.example.member.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.member.dto.KakaoUserInfoResponse;
import com.example.member.entity.Member;
import com.example.member.repository.MemberRepository;
import com.example.member.service.KakaoService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/kakao")
public class KakaoController {
	
	@Autowired
    private KakaoService kakaoService;
	@Autowired
	private MemberRepository memberRepository;
	
	@GetMapping("/login")
	public void loginKakao(HttpServletResponse response) throws IOException {
		String kakaoUrl = "https://kauth.kakao.com/oauth/authorize?client_id=60436ff86cba6a6503953dcc36304a02&redirect_uri=http://34.158.208.117:8080/member/login/kakao/callback&response_type=code";
		log.info("!!!!! [진짜 IP 확인용 로그] !!!!! : " + kakaoUrl);
		log.info("---------> [STEP 1] 카카오 로그인 페이지로 리다이렉트");
	    response.sendRedirect(kakaoUrl);
	}

	@GetMapping("/login/callback")
	public void kakaoCallback(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
	    
	    log.info("---------> [STEP 2] 카카오 인증 코드 수신: " + code);

	    // 1. 토큰 받아오고 정보 가져오기
	    String accessToken = kakaoService.getAccessToken(code);
	    KakaoUserInfoResponse userInfo = kakaoService.getUserInfo(accessToken);
	    
		@SuppressWarnings("all")
	    // 2. DB 확인 및 저장
	    Member member = memberRepository.findByKakaoId(userInfo.getId())
	            .map(existingMember -> {
	                existingMember.setNickname(userInfo.getKakao_account().getProfile().getNickname());
	                existingMember.setProfileImageUrl(userInfo.getKakao_account().getProfile().getProfile_image_url());
	                return memberRepository.save(existingMember);
	            })
	            .orElseGet(() -> {
	                return memberRepository.save(Member.builder()
	                        .kakaoId(userInfo.getId())
	                        .nickname(userInfo.getKakao_account().getProfile().getNickname())
	                        .profileImageUrl(userInfo.getKakao_account().getProfile().getProfile_image_url())
	                        .build());
	            });
	    
	    // 3. 보안 키 및 JWT 토큰 생성
	    String secretKey = "your-very-very-secret-key-should-be-very-long-and-secure";
	    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
	    
	    String jwtToken = Jwts.builder()
	            .setSubject(member.getKakaoId().toString())
	            .setIssuedAt(new Date())
	            .setExpiration(new Date(System.currentTimeMillis() + 3600000))
	            .signWith(key)
	            .compact();
	    
	    // 4. [핵심] 리액트(Server 3)로 토큰을 실어서 돌려보내기!
	    String redirectUrl = "http://localhost:5173/?token=" + jwtToken;
	    log.info("---------> [성공] 리액트로 이동: " + redirectUrl);
	    
	    response.sendRedirect(redirectUrl);
	    
	}

}
