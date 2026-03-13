package com.example.admin.dto;

public record GraphData (
	String label, // "9월", "10월" 또는 "2026-01"
	Long amount, // 거래액
	Long fee // 수수료 (선택 사항)
) {}