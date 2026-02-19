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

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NaverService {
	
	@Value("${naver.client.id}")
	private String clientId;
	
	@Value("${naver.client.secret}")
    private String clientSecret;
	
	public String getAccessToken(String code, String state) {
		
		RestTemplate rt = new RestTemplate();
		
		HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("state", state);
        
        HttpEntity<MultiValueMap<String, String>> naverTokenRequest = new HttpEntity<>(params, headers);
        
        ResponseEntity<NaverTokenResponse> response = rt.exchange(
        		"https://nid.naver.com/oauth2.0/token",
                HttpMethod.POST,
                naverTokenRequest,
                NaverTokenResponse.class
        );
        
        return response.getBody().getAccess_token();
        
	}
	
	public NaverUserInfoResponse getUserInfo(String accessToken) {
		
		RestTemplate rt = new RestTemplate();
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + accessToken);
	    headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
	    
	    HttpEntity<MultiValueMap<String, String>> naverProfileRequest = new HttpEntity<>(headers);
	    
	    ResponseEntity<NaverUserInfoResponse> response = rt.exchange(
	    		"https://openapi.naver.com/v1/nid/me",
	            HttpMethod.POST,
	            naverProfileRequest,
	            NaverUserInfoResponse.class
	    );
	    
	    log.info("---------> [네이버 사용자 정보 수신]: " + response.getBody());
	    return response.getBody();
	    
	}
	
}
