package com.example.member.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.member.dto.GoogleUserInfoResponse;
import com.example.member.entity.Member;
import com.example.member.repository.MemberRepository;
import com.example.member.service.GoogleService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/google")
public class GoogleController {
	
	@Autowired
	private GoogleService googleService;
	@Autowired
	private MemberRepository memberRepository;
	
	@Value("${google.client.id}")
	private String clientId;
		
	@Value("${google.redirect.uri}")
	private String clientUri;
	
	@GetMapping("/login")
	public void loginGoogle(HttpServletResponse response) throws IOException {
		String url = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + clientUri
                + "&response_type=code"
                + "&scope=email profile openid";
		
		log.info("---------> [STEP 1] 구글 로그인 페이지로 리다이렉트");
        response.sendRedirect(url);
	}
	
	@GetMapping("/login/callback")
	public void googleCallback(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
		
		log.info("-----------> [구글] 인증 코드 수신: " + code);
		
		String accessToken = googleService.getAccessToken(code);
        GoogleUserInfoResponse userInfo = googleService.getUserInfo(accessToken);
        
        Member member = memberRepository.findByGoogleId(userInfo.getSub())
        		.map(existingMember -> {
        			existingMember.setNickname(userInfo.getGiven_name());
        			existingMember.setProfileImageUrl(userInfo.getPicture());
        			return memberRepository.save(existingMember);
        		})
        		.orElseGet(() -> {
        			return memberRepository.save(Member.builder()
        					.googleId(userInfo.getSub())
                            .nickname(userInfo.getName())
                            .profileImageUrl(userInfo.getPicture())
                            .build());
        		});
        
        String secretKey = "your-very-very-secret-key-should-be-very-long-and-secure";
	    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
	    
	    String jwtToken = Jwts.builder()
	            .setSubject(member.getGoogleId().toString())
	            .setIssuedAt(new Date())
	            .setExpiration(new Date(System.currentTimeMillis() + 3600000))
	            .signWith(key)
	            .compact();
        
	    String redirectUrl = "http://localhost:5173/?token=" + jwtToken;
	    log.info("---------> [성공] 리액트로 이동: " + redirectUrl);
	    
        response.sendRedirect(redirectUrl);
		
	}
	
}
