package com.example.admin.dto.user;

import java.util.List;

import com.example.admin.dto.PointHistoryDTO;
import com.example.admin.dto.PurchaseHistoryDTO;

import lombok.Data;

@Data
public class UserDetailPaymentResponseDTO {
    private int totalPurchases;
    private long pointBalance;
    private List<PurchaseHistoryDTO> purchaseHistory;
    private List<PointHistoryDTO> pointHistory;
}
