// src/main/java/com/example/admin/dto/SettlementDashboardResponse.java
package com.example.admin.dto.settlement;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record SettlementDashboardResponse(
    DashboardSummary summary,
    List<ArtistSettlementRow> artistSettlements
) {
    public record DashboardSummary(
        BigDecimal totalGrossAmount,
        BigDecimal totalPlatformFee,
        BigDecimal totalExpectedAmount,
        BigDecimal totalSettledAmount
    ) {}

    public record ArtistSettlementRow(
        Long artistId,
        String artistName,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal netAmount,
        String status,
        // 역직렬화 매핑 오류 방지를 위해 payment 측과 동일한 OffsetDateTime 타입 사용
        OffsetDateTime lastTransactionDate
    ) {}
}