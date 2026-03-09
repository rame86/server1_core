package com.example.artist.entity.constant;

public enum DonationStatus {
	READY, // 결제 요청 전
	PROCESSING, // 결제 진행 중
	COMPLETE, // 결제 완료
	FAIL // 결제 실패
}
