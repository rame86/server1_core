package com.example.common.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.member.dto.RedisMemberDTO;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public Optional<RedisMemberDTO> getMemberData(String memberId) {
        String key = "AUTH:MEMBER:" + memberId;
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);

        if(data.isEmpty()) {
            log.warn("Redis 데이터 없음 - Key: {}", key);
            return Optional.empty();
        }

        try{
        	Object balance = data.get("balance");
            RedisMemberDTO dto = new RedisMemberDTO();
            
            dto.setMemberId(Long.valueOf(memberId));
            dto.setToken((String)data.get("token"));
            dto.setRole((String)data.get("role"));
            dto.setBalance(Long.valueOf(balance.toString()));
            return Optional.of(dto);
        } catch(Exception e) {
        	log.error("Redis 데이터 변환 에러! memberId: {}", memberId, e);
            return Optional.empty();
        }
    }

}
