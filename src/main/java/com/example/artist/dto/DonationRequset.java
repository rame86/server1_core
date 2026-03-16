package com.example.artist.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DonationRequset {
	private Long artistId; // 누구에게
	private BigDecimal amount; // 얼마를
}
