package com.example.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;

@Entity
@Table(name="board_comment", schema = "board")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    private Long boardId;
    private Long memberId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE"; // 기본값을 ACTIVE로 설정
}

