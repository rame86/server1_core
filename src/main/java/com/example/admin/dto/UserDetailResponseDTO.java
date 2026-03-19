package com.example.admin.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDetailResponseDTO {
	private Long memberId;
    private String name;
    private String email;
    private String phone;          // (상세 전용)
    private String address;        // (상세 전용)
    private String lastLoginIp;    // (상세 전용)
    
    private int totalPurchases;    // (리스트에 있었지만 최신으로 새로 받기!)
    private long pointBalance;     // (리스트에 있었지만 최신으로 새로 받기!)
    private List<PurchaseHistoryDTO> purchaseHistory; // (상세 전용)
    private List<PointHistoryDTO> pointHistory;       // (상세 전용)
}
