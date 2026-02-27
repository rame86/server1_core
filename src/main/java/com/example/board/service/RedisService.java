package com.example.board.service;

import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.board.dto.RedisMemberDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper; // JSON 파싱용

    public Optional<RedisMemberDTO> getMemberData(String memberId) {
        String key = "AUTH:MEMBER:" + memberId;
        String json = redisTemplate.opsForValue().get(key);

        if(json == null) {
            log.warn("Redis 데이터 없음 - Key: {}", key);
            return Optional.empty();
        }

        try{
            RedisMemberDTO dto = objectMapper.readValue(json, RedisMemberDTO.class);
            return Optional.of(dto);
        } catch(JsonProcessingException e) {
            log.error("JSON 파싱 에러! memberId: {}", memberId, e);
            return Optional.empty();
        }
    }

}
