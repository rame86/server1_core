package com.example.admin.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminEventListDTO {
	private Long approvalId;
    private Long eventId; // 2서버 식별자 ID
    private String title; 
    private String status;
    private int stock; // ✨ Redis에서 실시간으로 긁어온 잔여석!
    private LocalDateTime createdAt; // 신청일
}
