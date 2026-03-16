package com.example.artist.dto;


public record PaymentResponseDTO<T> (
	String orderId,
	String status,
	String message,
	String type,
	T payload)
{}
