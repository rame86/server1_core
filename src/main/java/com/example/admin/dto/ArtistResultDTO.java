package com.example.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ArtistResultDTO {
	private Long approvalId;      // 승인요청 ID
    private String artistName;    // 엔티티의 requesterName (김민준)
    private String subCategory;   // ECHO · 발라드
    private String description;   // 한 줄 소개
    private String imageUrl;      // 프로필 이미지
    private String createdAt;     // 신청 일시    
    private String status;           // PENDING, CONFIRMED, FAILED
    private String rejectionReason;  // 거절 사유
}
