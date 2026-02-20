package com.example.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {
	
	private final SecretKey key;
    private final long validityInMilliseconds = 3600000; // 1시간
    
    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
    	this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
    
    // 토큰 생성
    public String createToken(String subject) {
    	return Jwts.builder()
    			.setSubject(subject)
    			.setIssuedAt(new Date())
    			.setExpiration(new Date(System.currentTimeMillis() + validityInMilliseconds))
    			.signWith(key)
    			.compact();
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

}
