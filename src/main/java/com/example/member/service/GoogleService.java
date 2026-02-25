package com.example.member.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.example.member.dto.GoogleUserInfoResponse;
import com.example.member.dto.NaverTokenResponse;
import com.example.member.dto.OAuthUserInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleService implements OAuthProvider {
	
	private final RestTemplate restTemplate;
	
	@Value("${google.client.id}")
	private String clientId;
	
	@Value("${google.client.secret}")
    private String clientSecret;
	
	@Value("${google.redirect.uri}")
	private String clientUri;
	
	@Override
	public String getProviderName() { return "google"; }
	
	@Override
	public OAuthUserInfo getSocialUserInfo(String code, String state) {
		String token = getAccessToken(code);
		GoogleUserInfoResponse userInfo = getUserInfo(token);
		return userInfo;
	}

	public String getAccessToken(String code) {
		
		HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("redirect_uri", clientUri);
        
        HttpEntity<MultiValueMap<String, String>> googleTokenRequest = new HttpEntity<>(params, headers);
        ResponseEntity<NaverTokenResponse> response = restTemplate.exchange(
        		"https://oauth2.googleapis.com/token",
                HttpMethod.POST,
                googleTokenRequest,
                NaverTokenResponse.class
        );
        
        return response.getBody().getAccess_token();
		
	}
	
	public GoogleUserInfoResponse getUserInfo(String accessToken) {
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + accessToken);
	    headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
	    
	    HttpEntity<MultiValueMap<String, String>> googleProfileRequest = new HttpEntity<>(headers);
	    
	    ResponseEntity<GoogleUserInfoResponse> response = restTemplate.exchange(
	    		"https://www.googleapis.com/oauth2/v3/userinfo",
	            HttpMethod.POST,
	            googleProfileRequest,
	            GoogleUserInfoResponse.class
	    );
	    
	    log.info("---------> [구글 사용자 정보 수신]: " + response.getBody());
	    return response.getBody();
	    
	}

	@Override
	public String getAuthUrl() {
		return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + clientUri
                + "&response_type=code"
                + "&scope=email profile openid";
	}
	
}
