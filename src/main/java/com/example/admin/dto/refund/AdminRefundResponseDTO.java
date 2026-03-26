package com.example.admin.dto.refund;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AdminRefundResponseDTO {
	private String targetId; // 요청받았던 예약/주문 번호
    private String category; // "RES" 또는 "SHOP"
    private String status; // "APPROVED" (승인) 또는 "REJECTED" (반려)
    private String reason; // 반려 시 사유 (승인 시엔 비워둬도 됨)
    private Long adminId; // 승인한 관리자 ID (기록용)
}
