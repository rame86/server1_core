package com.example.admin.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistAccountResponse {
	private BigDecimal totalBalance;
    private BigDecimal withdrawableBalance;
}
