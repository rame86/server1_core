package com.example.board.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "report_board", schema = "board") // DB 테이블 이름
public class ReportBoard {

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

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist // 데이터가 저장되기 직전에 현재 시간을 자동으로 넣어줍니다.
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public ReportBoard(Long boardId, Long memberId, String reason) {
        this.boardId = boardId;
        this.memberId = memberId;
        this.reason = reason;
    }
}