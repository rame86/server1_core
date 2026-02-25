package com.example.member.service;

import com.example.member.dto.OAuthUserInfo;

public interface OAuthProvider {
	// 어떤 소셜인지 알려주는 메서드 (kakao, naver 등)
    String getProviderName();
	// 소셜 로그인 공통 메서드 (인터페이스 하나로 다 통하게!)
    OAuthUserInfo getSocialUserInfo(String code, String state);
    // 로그인 페이지 주소를 반환
    String getAuthUrl();
}
