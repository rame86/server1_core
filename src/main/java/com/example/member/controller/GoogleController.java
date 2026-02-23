package com.example.member.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.member.dto.GoogleUserInfoResponse;
import com.example.member.service.GoogleService;
import com.example.member.service.OAuthService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/google")
public class GoogleController {
	
	private final GoogleService googleService;
	private final OAuthService oauthService;
	
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
		String accessToken = googleService.getAccessToken(code);
        GoogleUserInfoResponse userInfo = googleService.getUserInfo(accessToken);
        oauthService.memberLogin(userInfo, response);
	}
	
}
