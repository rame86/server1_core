package com.example.member.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // 파라미터가 없는 기본 생성자(JSON을위해 사용)
public class NaverUserInfoResponse implements OAuthUserInfo {

	private String resultcode;
	private String message;
	private Response response; // 네이버는 'response' 키 안에 실제 정보가 있음
	
	@Data
	@NoArgsConstructor
	public static class Response {
        private String id;           // 네이버 고유 식별자
        private String nickname;     // 사용자 닉네임
        private String profile_image; // 프로필 이미지 URL
        private String email;        // 이메일
        private String name;         // 실명
    }

	@Override
	public String getProviderId() { return response.getId(); }
	@Override
	public String getEmail() { return response.getEmail(); }
	@Override
	public String getNickname() { return response.getNickname(); }
	@Override
	public String getProvider() { return "naver"; }
	
}
