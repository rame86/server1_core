package com.example.common.resolver;

import com.example.board.dto.RedisMemberDTO;
import com.example.board.service.RedisService;
import com.example.common.annotation.LoginUser;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component // 스프링이 이 클래스를 관리하도록 빈(Bean)으로 등록합니다.
@RequiredArgsConstructor // redisService를 주입받기 위해 사용합니다.
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final RedisService redisService;

    // [검사관 역할] 어떤 파라미터에 이 로직을 적용할지 결정합니다.
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 1. 파라미터 앞에 @LoginUser가 붙어 있는가?
        boolean hasAnnotation = parameter.hasParameterAnnotation(LoginUser.class);
        // 2. 파라미터의 타입이 RedisMemberDTO인가?
        boolean isMemberType = RedisMemberDTO.class.isAssignableFrom(parameter.getParameterType());
        
        return hasAnnotation && isMemberType;
    }

    // [실무자 역할] 실제로 데이터를 가져와서 리턴합니다. 리턴된 값이 파라미터로 들어갑니다.
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        
        // 1. SecurityContext에서 현재 로그인한 사용자의 인증 정보를 가져옵니다.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // 인증 정보가 없거나 익명 사용자인 경우 빈 상자(null)를 줍니다.
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            throw new RuntimeException("AUTH_REQUIRED");
        }

        String memberId = auth.getName(); // 우리가 로그에서 봤던 "10"이 여기 담깁니다.

        // 2. 아까 만들어둔 RedisService를 이용해 데이터를 가져옵니다.
        // Optional을 이미 적용하셨으므로 .orElse(null) 등을 사용하여 값을 꺼내 반환합니다.
        return redisService.getMemberData(memberId)
                .orElseThrow(() -> new RuntimeException("AUTH_REQUIRED"));
    }
}
