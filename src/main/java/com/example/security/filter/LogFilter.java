package com.example.security.filter;

import java.io.IOException;

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
@WebFilter(urlPatterns = { "/member/*" })
public class LogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        // [STEP 1: 형변환]
        // 기본 ServletRequest는 기능이 제한적이므로, HTTP 프로토콜 전용 기능을 쓰기 위해 변환합니다.
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // [STEP 2: 전처리 (요청 로그)]
        // 서블릿(Controller)으로 들어가기 '전'에 실행됩니다.
        // 누가 어디로 들어왔는지 기록을 남깁니다.
        log.info("---------> 요청 URI: " + httpRequest.getRequestURI());
        log.info("---------> 요청 전송방식: " + httpRequest.getMethod());

        // [STEP 3: 다음 단계로 전달 (핵심)]
        // 문지기가 문을 열어주는 단계입니다.
        // 이 코드가 실행되어야 실제 Controller의 메소드가 실행됩니다.
        chain.doFilter(request, response); 

        // [STEP 4: 후처리 (응답 로그)]
        // Controller가 모든 일을 마치고 응답이 다시 필터로 돌아온 시점입니다.
        // 작업이 성공했는지(200), 실패했는지(404, 500) 상태를 기록합니다.
        log.info("=========> 응답 상태: " + httpResponse.getStatus());
    }

}