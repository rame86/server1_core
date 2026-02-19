package com.example.member.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NaverTokenResponse {
	private String access_token;   // 접근 토큰 (이게 핵심!)
    private String refresh_token;  // 접근 토큰 만료 시 재발급용 토큰
    private String token_type;     // 토큰 타입 (보통 Bearer)
    private String expires_in;     // 토큰 유효 기간 (초 단위)
    private String error;          // 에러 코드 (실패 시)
    private String error_description; // 에러 메시지 (실패 시)
}
