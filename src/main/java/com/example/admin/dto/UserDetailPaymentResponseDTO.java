package com.example.admin.dto;

import java.util.List;

import lombok.Data;

@Data
public class UserDetailPaymentResponseDTO {
    private int totalPurchases;
    private long pointBalance;
    private List<PurchaseHistoryDTO> purchaseHistory;
    private List<PointHistoryDTO> pointHistory;
}
