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
	private Long targetId; // 2서버의 원본 데이터ID
	private String title;
	private String status; // PENDING, CONFIRMED, REJECTED
	
	@Column(columnDefinition = "TEXT")
	private String contentJson;
	
	private Long artistId; // 신청자 ID
	private Long adminId; //관리자 ID
	private String rejectionReason; // 거절 사유
	
	@CreationTimestamp
	private LocalDateTime createdAt;
	
	private LocalDateTime processedAt; // 승인, 거절 처리 일시

}
