package com.example.test.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired; // 추가
import org.springframework.stereotype.Service;

import com.example.test.dto.TestDto;

@Service
public class TestService {

    @Autowired // 매퍼 주입
    private TestMapper testMapper; // final 제거

    public List<TestDto> getAllTestData() {
        // 비즈니스 로직 및 예외 처리 가능
        return testMapper.findAll();
    }
}