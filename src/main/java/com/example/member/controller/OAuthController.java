package com.example.member.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.member.dto.OAuthUserInfo;
import com.example.member.service.OAuthProvider;
import com.example.member.service.OAuthService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/login")
public class OAuthController {
	
	// 모든 OAuthProvider 구현체들이 Map으로 자동 주입됩니다.
    // key: 빈 이름 (googleService, naverService 등)
	private final Map<String, OAuthProvider> oauthProviders;
	private final OAuthService oauthService;
	
	@GetMapping("/{provider}")
	public void loginProvider(@PathVariable("provider") String provider, HttpServletResponse response) throws IOException {
		OAuthProvider oAuthProvider = findProvider(provider);
		String authUrl = oAuthProvider.getAuthUrl();
		log.info("---------> [STEP 1] {} 로그인 페이지로 리다이렉트: {}", provider, authUrl);
	    response.sendRedirect(authUrl);
	}

	@GetMapping("/{provider}/callback")
	public void login(
			@PathVariable("provider") String provider,
			@RequestParam("code") String code,
			@RequestParam(value = "state", required = false) String state,
			HttpServletResponse response) throws IOException {
		OAuthProvider oAuthProvider = findProvider(provider);
		OAuthUserInfo userInfo = oAuthProvider.getSocialUserInfo(code, state);
		oauthService.memberLogin(userInfo, response);
	}
	
	private OAuthProvider findProvider(String provider) {
		return oauthProviders.values().stream()
				.filter(p -> p.getProviderName().equals(provider))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("지원하지 않는 로그인 방식입니다."));
	}
	
}