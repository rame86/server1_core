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
public class ArtistResponseDTO {
	
	// 기본 정보 (신청서, 회원정보 기반)
	private Long approvalId;      
    private Long artistId;
    private String artistName;    
    private String category;   
    private String description;   
    private String imageUrl;      
    private String createdAt;   
    private String status;
    
    // 활동 통계
    private Long followerCount;
    private java.math.BigDecimal totalBalance;
    private java.math.BigDecimal withdrawableBalance;
    
    // 관리자 처리
    private String rejectionReason;
    private Long adminId;
    private String processedAt;

}
