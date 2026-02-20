package com.example.member.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.member.dto.KakaoUserInfoResponse;
import com.example.member.service.KakaoService;
import com.example.member.service.OAuthService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/kakao")
public class KakaoController {
	
    private final KakaoService kakaoService;
	private final OAuthService oauthService;
	
	@Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;
    
	@GetMapping("/login")
	public void loginKakao(HttpServletResponse response) throws IOException {
		String url = "https://kauth.kakao.com/oauth/authorize"
				+ "?client_id=" + clientId
				+ "&redirect_uri=" + redirectUri
				+ "&response_type=code";
		
		log.info("---------> [STEP 1] 카카오 로그인 페이지로 리다이렉트");
	    response.sendRedirect(url);
	}

	@GetMapping("/login/callback")
	public void kakaoCallback(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
	    String accessToken = kakaoService.getAccessToken(code);
	    KakaoUserInfoResponse userInfo = kakaoService.getUserInfo(accessToken);
	    oauthService.memberLogin(userInfo, response);
	}

}
