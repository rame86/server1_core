package com.example.member.dto;

import lombok.Data;

@Data
public class KakaoUserInfoResponse {
	
	private Long id; // 카카오 회원 고유번호
    private KakaoAccount kakao_account;

    @Data
    public static class KakaoAccount {
        private String email;
        private Profile profile;
    }

    @Data
    public static class Profile {
        private String nickname;
        private String profile_image_url;
    }

}