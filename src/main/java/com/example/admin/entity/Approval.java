package com.example.admin.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "approval", schema = "approval")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Approval {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long approvalId;
	
	private String category;  // EVNET, SHOTP, PAY 등
	private Long targetId; // 2서버의 PK
	private String title; // 리스트에 보여줄 제목
	private String status; // PENDING, CONFIRMED, FAILED(거절)
	
	private String location; // 장소
	private Long price; // 금액
	private Integer stock; // 재고

	@Column(columnDefinition = "TEXT")
	private String imageUrl; // 썸네일(?)
	
	private String requesterName; //신청자 닉네임
	private String subCategory; // 공식굿즈, 팬메이드 등등....

	@Column(columnDefinition = "TEXT")
	private String description; // 간단한 설명
	
	@Column(name = "event_start_date")
    private LocalDateTime eventStartDate; //이벤트 시작일
	
	@Column(columnDefinition = "TEXT")
	private String contentJson; // 위 필드를 제외한 나머지 상세 정보
	
	private Long artistId; // 신청자 ID
	private Long adminId; //관리자 ID
	private String rejectionReason; // 거절 사유
	
	@CreationTimestamp
	private LocalDateTime createdAt;
	private LocalDateTime processedAt; // 승인, 거절 처리 일시

}
