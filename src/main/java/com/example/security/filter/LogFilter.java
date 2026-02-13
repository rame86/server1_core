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

// [설정] 롬복(Lombok)을 사용해 log 변수를 자동으로 생성합니다.
@Slf4j
// [설정] 필터가 동작할 URL 범위를 지정합니다. (/member/로 시작하는 모든 주소 감시)
@WebFilter(urlPatterns = { "/*" })
public class LogFilter implements Filter {
	
	// 토큰없이도 접근 가능한 명단
	private static final String[] whiteList = {"/", "/event", "/dbtest", "/member/*"};
	// 키
	private String secretKey = "your-very-very-secret-key-should-be-very-long-and-secure";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        // [STEP 1: 형변환]
        // 기본 ServletRequest는 기능이 제한적이므로, HTTP 프로토콜 전용 기능을 쓰기 위해 변환합니다.
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 화이트리스트
        if(isWhiteList(requestURI)) {
        	log.info("--------->  [화이트리스트] 프리패스: " + requestURI);
        	chain.doFilter(request, response);
        	return;
        }
        
        // [STEP 2: 전처리 (요청 로그)]
        // 서블릿(Controller)으로 들어가기 '전'에 실행됩니다.
        // 누가 어디로 들어왔는지 기록을 남깁니다.
        log.info("---------> 요청 URI: " + httpRequest.getRequestURI());
        log.info("---------> 요청 전송방식: " + httpRequest.getMethod());
        log.info("---------> [인증 필요] 검사 시작: " + requestURI);
        
        // 헤어데서 토큰 꺼내기
        String authHeader = httpRequest.getHeader("Authorization");
        
        try {
        	
        	// 토큰이 없거나 이상할 시
        	if(authHeader == null || !authHeader.startsWith("Bearer ")) throw new Exception("토큰이 없습니다");
        	
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
        	
        	// [실패] 토큰이 가짜거나, 만료됐거나, 없을 때 실행됨
            log.error("[인증 실패] 사유: " + e.getMessage());
            
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 에러
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
            
            return;
            
		}

        // [STEP 4: 후처리 (응답 로그)]
        // Controller가 모든 일을 마치고 응답이 다시 필터로 돌아온 시점입니다.
        // 작업이 성공했는지(200), 실패했는지(404, 500) 상태를 기록합니다.
        log.info("=========> 응답 상태: " + httpResponse.getStatus());
    }
    
    
    // 화이트리스트 체크로직
    private boolean isWhiteList(String requestURI) {
    	// 스프링이 제공하는 도구가 별표(*) 패턴을 알아서 계산해줌!! 개사기!!!!
    	return PatternMatchUtils.simpleMatch(whiteList, requestURI);
    }

}