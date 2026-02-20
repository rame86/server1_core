package com.example.member.dto;

public interface OAuthUserInfo {
	String getProviderId(); // 각 플랫폼 고유의 ID
	String getEmail();
    String getNickname();
    String getProvider(); // "google", "naver", "kakao"
}