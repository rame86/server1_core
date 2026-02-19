package com.example.member.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class NaverUserInfoResponse {

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
	
}
