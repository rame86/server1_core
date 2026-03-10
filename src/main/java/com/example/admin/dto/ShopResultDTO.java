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
public class ShopResultDTO {
	
    private Long approvalId; // 승인요청 ID
    private Long goodsId; // 굿즈상품 ID
    private Long requesterId; // 신청자 ID
    private String requesterName; // 화면에 나타날 닉네임
    
    private String goodsName; // 상품명
    private String goodsType; // 상품 타입
    private String description; // 상품 설명
    private Integer price; // 가격
    private Integer stock; // 재고
    private String imageUrl; // 상품 이미지 경로
    
    private String status; // 상태 (PENDING, CONFIRMED, FAILED)
    private String rejectionReason; // 거절 사유
    private String createdAt; // 신청 일자
    
}
