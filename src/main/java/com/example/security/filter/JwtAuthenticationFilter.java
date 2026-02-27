package com.example.security.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.security.tokenProvider.JwtTokenProvider;

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

    private static final String[] whiteList = {
    		"/", 
    		"/event", 
    		"/member/**", 
    		"/dbtest", 
    		"/api/**", 
    		"/test/**"
    };

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
        
        // 토큰 추출
        String token = jwtTokenProvider.resolveToken(request);
        try {
        	if(token != null && jwtTokenProvider.validateToken(token)) {
        		String memberId = jwtTokenProvider.getSubject(token);
        		String role = jwtTokenProvider.getRole(token);
        		
        		UsernamePasswordAuthenticationToken authentication =
        				new UsernamePasswordAuthenticationToken(memberId, null, 
        						List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        		
        		SecurityContextHolder.getContext().setAuthentication(authentication);
        		log.info("---------> [AUTH] Lua 검증 완료 신뢰, 내부 인증 객체 생성 완료: {}", memberId);
        	}
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