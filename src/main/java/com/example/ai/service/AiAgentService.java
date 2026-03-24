package com.example.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.ai.dto.AiRequest;
import com.example.ai.dto.AiResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiAgentService {
	
	private final RestTemplate restTemplate;
	
	public String getAiAnswer(String message) {
		String url = "http://localhost:8000/chat"; // 로컬 파이썬 에이전트 주소
		try {
            AiRequest request = new AiRequest(message);
            AiResponse response = restTemplate.postForObject(url, request, AiResponse.class);
            return response != null ? response.answer() : "AI가 대답을 하지 않네요..";
        } catch (Exception e) {
            return "AI 서버 연결 실패: " + e.getMessage();
        }
	}
	
}
