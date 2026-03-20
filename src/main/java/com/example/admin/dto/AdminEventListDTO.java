package com.example.admin.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminEventListDTO {
	private Long approvalId;
	private String artistname;
    private Long targetId; // 2서버 식별자 ID
    private String title; 
    private String status;
    private String category;
    private int stock; // ✨ Redis에서 실시간으로 긁어온 잔여석!
    private String location;
    private Long price;
    private LocalDateTime eventStartDate;
    private LocalDateTime createdAt; // 신청일
    private String imageUrl;
}
