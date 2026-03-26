package com.example.config;

import com.example.security.filter.JwtAuthenticationFilter;
import com.example.security.filter.LogFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        // ⭐ 바로 이 줄이 핵심입니다! 비동기 요청을 허용합니다.
        registration.setAsyncSupported(true); 
        registration.setOrder(1); // 필터 순서 (필요에 따라 조절)
        return registration;
    }

    @Bean
    public FilterRegistrationBean<LogFilter> logFilterRegistration(LogFilter filter) {
        FilterRegistrationBean<LogFilter> registration = new FilterRegistrationBean<>(filter);
        // ⭐ 로그 필터도 비동기를 지원해야 에러가 안 납니다.
        registration.setAsyncSupported(true); 
        registration.setOrder(0); // 로그를 가장 먼저 찍으려면 낮은 숫자
        return registration;
    }
}
