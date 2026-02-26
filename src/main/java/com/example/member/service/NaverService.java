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

import com.example.member.dto.NaverTokenResponse;
import com.example.member.dto.NaverUserInfoResponse;
import com.example.member.dto.OAuthUserInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class NaverService implements OAuthProvider{
	
	private final RestTemplate restTemplate;
	
	@Value("${naver.client.id}")
	private String clientId;
	
	@Value("${naver.client.secret}")
    private String clientSecret;
	
	@Value("${naver.redirect.uri}")
    private String redirectUri;
	
	@Override
	public String getProviderName() { return "naver"; }

	@Override
	public OAuthUserInfo getSocialUserInfo(String code, String state) {
		String token = getAccessToken(code, state);
		NaverUserInfoResponse userInfo = getUserInfo(token);
		return userInfo;
	}
	
	public String getAccessToken(String code, String state) {

		HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("state", state);
        
        HttpEntity<MultiValueMap<String, String>> naverTokenRequest = new HttpEntity<>(params, headers);
        
        ResponseEntity<NaverTokenResponse> response = restTemplate.exchange(
        		"https://nid.naver.com/oauth2.0/token",
                HttpMethod.POST,
                naverTokenRequest,
                NaverTokenResponse.class
        );
        
        return response.getBody().getAccess_token();
        
	}
	
	public NaverUserInfoResponse getUserInfo(String accessToken) {
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + accessToken);
	    headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
	    
	    HttpEntity<MultiValueMap<String, String>> naverProfileRequest = new HttpEntity<>(headers);
	    
	    ResponseEntity<NaverUserInfoResponse> response = restTemplate.exchange(
	    		"https://openapi.naver.com/v1/nid/me",
	            HttpMethod.POST,
	            naverProfileRequest,
	            NaverUserInfoResponse.class
	    );
	    
	    log.info("---------> [네이버 사용자 정보 수신]: " + response.getBody());
	    return response.getBody();
	    
	}

	@Override
	public String getAuthUrl() {
		String state = "RANDOM_STATE_STRING"; 
		return "https://nid.naver.com/oauth2.0/authorize?response_type=code"
        + "&client_id=" + clientId
        + "&redirect_uri=" + redirectUri
        + "&state=" + state;
	}
	
}
