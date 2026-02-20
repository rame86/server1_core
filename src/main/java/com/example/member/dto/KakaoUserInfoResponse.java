package com.example.member.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // 파라미터가 없는 기본 생성자(JSON을위해 사용)
public class KakaoUserInfoResponse implements OAuthUserInfo {
	
	private String id; // 카카오 회원 고유번호
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

	@Override
	public String getProviderId() { return getId(); }
	@Override
	public String getEmail() { return kakao_account.getEmail(); }
	@Override
	public String getNickname() {  return kakao_account.profile.getNickname(); }
	@Override
	public String getProvider() { return "kakao"; }

}