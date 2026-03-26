package com.example.admin.dto.settlement;

public record ArtistSettlement (
	String artistName, // 아티스트 이름
	Long totalTransaction, // 총 거래액
	Long fee, // 수수료
	Long netAmount, // 정산액 (거래액 - 수수료)
	String status, // "완료" 또는 "예정"
	String settlementDate // "2026-02-10"
) {}