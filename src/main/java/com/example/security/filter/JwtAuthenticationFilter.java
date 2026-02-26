package com.example.security.filter;

import java.io.IOException;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.security.JwtTokenProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component // 스프링 빈으로 등록하여 Repository 주입 가능하게 함
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	
	private final JwtTokenProvider jwtTokenProvider;
	private final StringRedisTemplate redisTemplate;
<<<<<<< HEAD
    private static final String[] whiteList = {"/", "/event", "/member/*", "/dbtest", "/msa/core/**", "/signup/*", "/signup.html","/api/**"};
=======
    private static final String[] whiteList = {"/", "/event", "/msa/core/member/**", "/dbtest", "/msa/core/api/login/**"};
>>>>>>> 1408f8c1225bad91bdfafd5e080de7978f8c3c9c
    
	@Override
	protected void doFilterInternal(HttpServletRequest request, 
			HttpServletResponse response, 
			FilterChain filterChain) throws ServletException, IOException {
		
		String requestURI = request.getRequestURI();
		
        // 화이트리스트는 검사 안 하고 통과
        if (isWhiteList(requestURI)) {
        	log.info("--------->  [화이트리스트] 프리패스: " + requestURI);
        	filterChain.doFilter(request, response);
            return;
        }
        
        try {
        	
        	// 헤더에서 토큰 추출
        	log.info("---------> [AUTH] 인증 검사 시작: {}", requestURI);
        	String authHeader = request.getHeader("Authorization");
        	if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("토큰이 존재하지 않습니다.");
            }
        	
        	// 토큰 유효성 검사
        	String token = authHeader.substring(7);
        	if(!jwtTokenProvider.validateToken(token)) {
        		throw new RuntimeException("유효하지 않은 토큰입니다.");
        	}
        	
        	// Redis에 토큰이 살아있는지 확인
        	String redisKey = "TOKEN:" + token;
        	String userInfo = redisTemplate.opsForValue().get(redisKey);
        	if(userInfo == null) {
        		log.error("---------> [AUTH] Redis에 토큰이 없음 (만료되었거나 로그아웃됨)");
        		throw new RuntimeException("로그아웃되었거나 만료된 세션입니다.");
        	}
        	
        	// Redis에 값이 있다면 확인 가능
        	log.info("---------> [AUTH] Redis 확인 완료: {}", userInfo);
        	log.info("---------> [인증성공] 유저 정보: {}", userInfo);
        	filterChain.doFilter(request, response);
        	
        } catch (Exception e) {
        	log.error("---------> [AUTH] 인증 실패: {}", e.getMessage());
        	
        	// 응답 상태를 401(Unauthorized)로 설정
        	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        	
        	// 리액트가 읽을 수 있도록 JSON 형식으로 에러 메시지 작성
        	response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"" + e.getMessage() + "\"}");
            
            return;
        }
        
	}

    // 화이트리스트 체크로직
    private boolean isWhiteList(String requestURI) {
    	// 스프링이 제공하는 도구가 별표(*) 패턴을 알아서 계산해줌!! 개사기!!!!
        return PatternMatchUtils.simpleMatch(whiteList, requestURI);
    }
    
}