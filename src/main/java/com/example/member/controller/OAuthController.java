package com.example.member.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.member.dto.OAuthUserInfo;
import com.example.member.service.OAuthService;
import com.example.member.service.provider.OAuthProvider;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/login")
public class OAuthController {
	
	@Value("${sign.up.url}")
	private String signUpUrl;
	
	@Value("${login.user.url}")
	private String loginUrl;
	
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
		
		// 1. 소셜 업체(카카오, 구글 등)로부터 유저 정보를 가져옴
		OAuthProvider oAuthProvider = findProvider(provider);
		OAuthUserInfo userInfo = oAuthProvider.getSocialUserInfo(code, state);
		
		// 2. 서비스에 유저 정보를 던져서 "로그인 데이터"를 받아옴
		Map<String, Object> result = oauthService.memberLogin(userInfo, response);
		
		// 3. 서비스 결과에 따라 리다이렉트 결정
		if("signup".equals(result.get("status"))) {
			log.info("---------> [STEP 2] 신규 유저 가입 페이지로 리다이렉트");
			signupRedirect(response, (OAuthUserInfo)result.get("userInfo"));
		} else {
			log.info("---------> [STEP 2] 기존 유저 로그인 처리 및 리다이렉트");
			loginRedirect(response, result);
		}
		
	}
	
	private OAuthProvider findProvider(String provider) {
		return oauthProviders.values().stream()
				.filter(p -> p.getProviderName().equals(provider))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("지원하지 않는 로그인 방식입니다."));
	}
	
	// 회원가입 페이지로 리다이렉트
	private void signupRedirect(HttpServletResponse response, OAuthUserInfo userInfo) throws IOException {
		String email = (userInfo.getEmail() != null) ? userInfo.getEmail() : "";
		String nickName = "";
		if(userInfo.getNickname() != null) nickName = java.net.URLEncoder.encode(userInfo.getNickname(), java.nio.charset.StandardCharsets.UTF_8);
		
		String redirectUrl = signUpUrl
        		+ "?email=" + email
        		+ "&nickname=" + nickName
        		+ "&provider=" + userInfo.getProvider()
    			+ "&providerId=" + userInfo.getProviderId();
       	response.sendRedirect(redirectUrl);
	}
	
	// 로그인 성공시 리다이렉트
	private void loginRedirect(HttpServletResponse response, Map<String, Object> loginData) throws IOException {
		String redirectUrl = loginUrl
				+ loginData.get("token")
				+ "&member_id=" + loginData.get("member_id")
				+ "&role=" + loginData.get("role");
		response.sendRedirect(redirectUrl);
	}
	
}