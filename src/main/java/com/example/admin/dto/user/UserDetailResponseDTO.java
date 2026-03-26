package com.example.admin.dto.user;

import java.util.List;

import com.example.admin.dto.PointHistoryDTO;
import com.example.admin.dto.PurchaseHistoryDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDetailResponseDTO {
	private Long memberId;
    private String name;
    private String email;
    private String status;
    
    private String phone; // (상세 전용)
    private String address; // (상세 전용)
    private String lastLoginIp; // (상세 전용)
    private String lastLoginAt; // (상세 전용)
    private String adminMemo; // (상세 전용)
    
    private int totalPurchases;
    private long pointBalance;
    private List<PurchaseHistoryDTO> purchaseHistory; // 구매이력
    private List<PointHistoryDTO> pointHistory;       // 포인트이력
}
