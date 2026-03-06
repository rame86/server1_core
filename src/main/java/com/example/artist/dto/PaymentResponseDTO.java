package com.example.artist.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PaymentResponseDTO {
	private String orderId;
	private String status;
	private String message;
	private String type;
}
