package com.example.board.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "board", schema = "board") // DB 테이블 이름
public class BoardReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id") // DB의 report_id와 매핑 필수
    private Long reportId;

    @Column(name = "board_id", nullable = false) // 컬럼명 명시
    private Long boardId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist // 데이터가 저장되기 직전에 현재 시간을 자동으로 넣어줍니다.
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if(this.status == null) this.status = "PENDING"; // 저장 시 기본값 설정
    }

    // 승인 상태 변경 메서드
    public void approve(){
        this.status = "APPROVED";
    }
}