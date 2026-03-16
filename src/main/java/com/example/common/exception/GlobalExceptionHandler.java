package com.example.common.exception;

import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
        // 리졸버가 던진 "AUTH_REQUIRED" 에러라면?
        if ("AUTH_REQUIRED".equals(e.getMessage())) {
            // 리액트가 화내지 않게 401 상태코드와 함께 빈 리스트[]를 보냅니다.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ArrayList<>());
        }
        return ResponseEntity.internalServerError().body("서버 내부 에러 발생");
    }
    
}
