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

import com.example.member.dto.NaverUserInfoResponse;
import com.example.member.entity.Member;
import com.example.member.repository.MemberRepository;
import com.example.member.service.NaverService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/naver")
public class NaverController {
	
	@Autowired
	private NaverService naverService;
	@Autowired
	private MemberRepository memberRepository;
	
	@Value("${naver.client.id}")
    private String clientId;

    @Value("${naver.redirect.uri}")
    private String redirectUri;

    @GetMapping("/login")
    public void loginNaver(HttpServletResponse response) throws IOException {
        // state: 보안을 위한 상태 토큰 (랜덤 문자열 추천)
        String state = "RANDOM_STATE_STRING"; 
        
        String naverUrl = "https://nid.naver.com/oauth2.0/authorize?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&state=" + state;

        log.info("---------> [STEP 1] 네이버 로그인 페이지로 리다이렉트");
        response.sendRedirect(naverUrl);
    }

    @GetMapping("/login/callback")
    public void naverCallback(@RequestParam("code") String code, 
                              @RequestParam("state") String state, 
                              HttpServletResponse response) throws IOException {
        
        log.info("---------> [STEP 2] 네이버 인증 코드 수신: " + code);
        
        String accessToken = naverService.getAccessToken(code, state);
        NaverUserInfoResponse userInfo = naverService.getUserInfo(accessToken);
        
        Member member = memberRepository.findByNaverId(userInfo.getResponse().getId())
        		.map(existingMember -> {
        			existingMember.setNickname(userInfo.getResponse().getNickname());
        	        existingMember.setProfileImageUrl(userInfo.getResponse().getProfile_image());
        	        return memberRepository.save(existingMember);
        		})
        		.orElseGet(() -> {
        			return memberRepository.save(Member.builder()
        	                .naverId(userInfo.getResponse().getId())
        	                .nickname(userInfo.getResponse().getNickname())
        	                .profileImageUrl(userInfo.getResponse().getProfile_image())
        	                .build());
        		});

        // 4. JWT 생성 후 리액트로 리다이렉트 (카카오랑 동일)
        String secretKey = "your-very-very-secret-key-should-be-very-long-and-secure";
	    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
	    
	    String jwtToken = Jwts.builder()
	            .setSubject(member.getNaverId().toString())
	            .setIssuedAt(new Date())
	            .setExpiration(new Date(System.currentTimeMillis() + 3600000))
	            .signWith(key)
	            .compact();
        
	    String redirectUrl = "http://localhost:5173/?token=" + jwtToken;
	    log.info("---------> [성공] 리액트로 이동: " + redirectUrl);
	    
        response.sendRedirect(redirectUrl);
        
    }
        
}
