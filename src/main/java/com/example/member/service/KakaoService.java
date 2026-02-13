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

import com.example.member.dto.KakaoTokenResponse;
import com.example.member.dto.KakaoUserInfoResponse;

@Service
public class KakaoService {
	
	@Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;
    
    @Value("${kakao.client-secret}")
    private String clientSecret;
    
    public String getAccessToken(String code) {
        RestTemplate rt = new RestTemplate();

        // 헤더 설정 (카카오가 정한 규칙)
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // 바디 설정 (우리가 챙겨갈 서류)
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("client_secret", clientSecret);
        params.add("code", code);

        // 요청 전송
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(params, headers);
        ResponseEntity<KakaoTokenResponse> response = rt.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                kakaoTokenRequest,
                KakaoTokenResponse.class
        );

        return response.getBody().getAccess_token();
    }
    
    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        RestTemplate rt = new RestTemplate();

        // 이번에는 헤더에 토큰을 담아서 보냅니다.
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken); // 앞에 'Bearer ' 한 칸 띄우는 거 필수!
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<MultiValueMap<String, String>> kakaoProfileRequest = new HttpEntity<>(headers);
        @SuppressWarnings("all")
        ResponseEntity<KakaoUserInfoResponse> response = rt.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                kakaoProfileRequest,
                KakaoUserInfoResponse.class
        );

        return response.getBody();
    }

}
