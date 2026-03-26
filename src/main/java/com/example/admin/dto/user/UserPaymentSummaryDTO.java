package com.example.admin.dto.user;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPaymentSummaryDTO {
	private Long memberId;
    private Integer purchaseCount;
    @JsonProperty("balance")
    private BigDecimal pointBalance;
}
