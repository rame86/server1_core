package com.example.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PointHistoryDTO {
	private String processedAt; // 처리 일시
    private String type; // 구분 (CHARGE: 충전, USE: 사용, REFUND: 환불)
    private Long amount; // 변동 금액 (충전이면 +, 사용이면 -로 표시하면 센스 굿! ㅡㅡ)
    private String description; // 상세 내용 (예: '아티스트 굿즈 A' 결제 사용, 계좌 충전 등)
    private Long remainBalance; // (선택) 처리 후 잔액 - 있으면 관리자가 보기 편해!
}
