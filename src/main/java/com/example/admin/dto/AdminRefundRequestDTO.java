package com.example.admin.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AdminRefundRequestDTO {
	
	// 1. 공통 필수 정보 (리스트 화면 노출용)
    private String category; // "RES" 또는 "SHOP" (어디서 온 환불인지 구분)
    private String type; // "REFUND" 고정
    private String targetId; // 고유 식별값 (예약번호 or 주문번호)
    private String title; // 관리자 화면 제목 (예: "[굿즈] 응원봉 환불 요청")
    private Long memberId; // 환불 신청한 회원 번호
    private Integer totalPrice; // 환불 예정 총 금액
    private String status; // 현재 상태 (PENDING 등)

    // 2. 상세 데이터 (이게 핵심!)
    // 여기에 Res 전용(티켓코드, 좌석번호)이나 Shop 전용(상품명, 수량) 정보를 담아!
    private Map<String, Object> contentJson;
    
    // 3. 부가 정보
    private String createdAt;   // 신청 일시

}
