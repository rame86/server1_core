package com.example.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER) // 이 이름표는 파라미터(매개변수) 앞에만 붙일 수 있다!
@Retention(RetentionPolicy.RUNTIME) // 프로그램이 실행될 때(Runtime)까지 이 이름표를 유지해라!
public @interface LoginUser {
    // 인터페이스 앞에 @가 붙으면 '이름표(어노테이션)'가 됩니다.
}