package com.example.security.filter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.util.PatternMatchUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@WebFilter(urlPatterns = { "/member/*" })
public class LoginCheckFilter implements Filter {
    
    private static final String[] whiteList = {"/", "/event", "/dbtest", "/member/*"};
    private final String secretKey = "your-very-very-secret-key-should-be-very-long-and-secure";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 1. 화이트리스트는 검사 안 하고 통과
        if (isWhiteList(requestURI)) {
        	log.info("--------->  [화이트리스트] 프리패스: " + requestURI);
            chain.doFilter(request, response);
            return;
        }

        // 2. 인증 검사 시작
        log.info("---------> [AUTH] 인증 검사 시작: {}", requestURI);
        // 헤어데서 토큰 꺼내기
        String authHeader = httpRequest.getHeader("Authorization");
        
        try {
        	// 토큰이 없거나 이상할 시
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new Exception("토큰이 없습니다.");
            }
            
            String token = authHeader.substring(7);
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            
         // JWT 해독 및 검증
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            String userId = claims.getSubject();
        	log.info("[인증 성공] 유저 ID: " + userId);
        	
            // 성공 시 다음으로 이동
            chain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("---------> [AUTH] 인증 실패: {}", e.getMessage());
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
            
            return;
        }
        
        log.info("=========> 응답 상태: " + httpResponse.getStatus());
    }

 // 화이트리스트 체크로직
    private boolean isWhiteList(String requestURI) {
    	// 스프링이 제공하는 도구가 별표(*) 패턴을 알아서 계산해줌!! 개사기!!!!
        return PatternMatchUtils.simpleMatch(whiteList, requestURI);
    }
}