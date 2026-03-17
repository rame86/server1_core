package com.example.admin.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ArtistAccountResponse {
	private BigDecimal totalBalance;
    private BigDecimal withdrawableBalance;
}
