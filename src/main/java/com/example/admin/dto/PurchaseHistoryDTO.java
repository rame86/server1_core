package com.example.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PurchaseHistoryDTO {
	private String purchasedAt; // 결제 일시 (예: 2026-03-18 15:30)
    private String itemName; // 상품명 (예: 아티스트 굿즈 A)
    private Long amount; // 결제 금액 (예: 25000)
    private String status; // 상태 (결제완료, 환불 등 - 선택사항)
}
