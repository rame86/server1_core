package com.example.security.tokenProvider;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtTokenProvider {
	
	private final SecretKey key;
	private final long accessTokenValidity = 1000L * 60 * 60; // 1시간
    private final long refreshTokenValidity = 1000L * 60 * 60 * 24 * 14; // 14일 (2주)
    
    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
    	this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
    
    // 토큰 생성
    private String buildToken(Long member_id, String role, String name, long validityTime) {
    	Claims claims = Jwts.claims().setSubject(String.valueOf(member_id));
    	claims.put("role", role);
    	claims.put("name", name);

    	Date now = new Date();
    	Date validity = new Date(now.getTime() + validityTime);
    	
    	return Jwts.builder()
    			.setClaims(claims)
    			.setIssuedAt(now)
    			.setExpiration(validity)
    			.signWith(key)
    			.compact();
    }
    
    // 기본 토큰
    public String createToken(Long member_id, String role, String name) {
    	return buildToken(member_id, role, name, accessTokenValidity);
    }
    
    // 리프래시 토큰
    public String refreshToken(Long member_id, String role, String name) {
    	return buildToken(member_id, role, name, refreshTokenValidity);
    }
    
    // 토큰이 유효한지 검증
    public boolean validateToken(String token) {
    	try {
    		Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token);
        return true;
    	} catch (Exception e) {
    		return false;
    	}
    }
    
    // 토큰에서 유저ID를 꺼냄
    public String getSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    
    // 토큰에서 Role(권한) 정보를 꺼냄
    public String getRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }
    
    // HTTP 요청 헤더에서 실제 토큰값만 추출
    public String resolveToken(HttpServletRequest request) {
    	String bearerToken = request.getHeader("Authorization");
    	if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
