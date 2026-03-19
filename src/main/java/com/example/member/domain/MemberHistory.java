package com.example.member.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "member_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberHistory {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;
	
	@Column(nullable = false)
    private Long memberId;
	
	@Column(nullable = false)
    private String status;
	
	@Column(columnDefinition = "TEXT")
    private String reason;
	
	private Long adminId;
	private LocalDateTime createdAt;
	
	@PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now(); // 저장할 때 자동으로 시간 찍기! 🚀
    }

}
