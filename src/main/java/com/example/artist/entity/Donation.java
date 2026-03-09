package com.example.artist.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.artist.entity.constant.DonationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "donation", schema = "donation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class) // 데이터가 생성되거나 수정될 때 시간을 자동으로 기록
public class Donation {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // 1부터 번호 자동 생성
	private Long id;
	
	@Column(nullable = false, unique = true)
	private String orderId;
	
	@Column(nullable = false)
	private Long memberId;
	
	@Column(nullable = false)
	private Long artistId;
	
	@Column(nullable = false)
	private BigDecimal amount;
	
	@Enumerated(EnumType.STRING)
	private DonationStatus status;
	
	@CreatedDate
	@Column(updatable = false)
	private LocalDateTime createdAt;
	
	@LastModifiedDate
	private LocalDateTime updatedAt;
	
	@Builder
	public Donation(String orderId, Long memberId, Long artistId, BigDecimal amount, DonationStatus status) {
		this.orderId = orderId;
        this.memberId = memberId;
        this.artistId = artistId;
        this.amount = amount;
        this.status = status;
	}
	
	public void processing() {
		this.status = DonationStatus.PROCESSING;
	}
	
	public void complete() {
		this.status = DonationStatus.COMPLETE;
	}
	
	public void ready() {
		this.status = DonationStatus.READY;
	}
	
	public void fail() {
        this.status = DonationStatus.FAIL;
    }
	
}
