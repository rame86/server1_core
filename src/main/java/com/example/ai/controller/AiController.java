package com.example.ai.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ai.service.AiAgentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
	
	private final AiAgentService aiAgentService;
	
	@PostMapping("/ask")
	public ResponseEntity<String> askToAi(@RequestBody Map<String, String> body) {
		String userMessage = body.get("message");
		String answer = aiAgentService.getAiAnswer(userMessage);
		return ResponseEntity.ok(answer);
	}

}
