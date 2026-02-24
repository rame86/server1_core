package com.example.member.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.member.dto.NaverUserInfoResponse;
import com.example.member.service.NaverService;
import com.example.member.service.OAuthService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/naver")
public class NaverController {
	
	private final NaverService naverService;
	private final OAuthService oauthService;
	
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
        String accessToken = naverService.getAccessToken(code, state);
        NaverUserInfoResponse userInfo = naverService.getUserInfo(accessToken);
        oauthService.memberLogin(userInfo, response);  
    }
    
}
