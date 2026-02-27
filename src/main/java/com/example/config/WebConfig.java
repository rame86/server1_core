package com.example.config;

import com.example.common.resolver.LoginUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration // 이 클래스가 스프링 설정 파일임을 알려줍니다.
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    // 2단계에서 만든 해결사를 주입받습니다.
    private final LoginUserArgumentResolver loginUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // 스프링이 컨트롤러 파라미터를 처리할 때 사용할 리스트에 우리 해결사를 추가합니다.
        resolvers.add(loginUserArgumentResolver);
    }
}
