package com.example.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;

@Entity
@Table(name = "board_comment", schema = "board")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA를 위한 기본 생성자
@AllArgsConstructor // 빌더를 위한 전체 생성자
@Builder // Lombok이 빌더 패턴 코드를 자동으로 생성함
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    // ManyToOne 연관관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board boardId;

    @Column(name = "member_id")
    private Long memberId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE"; 

    // [비즈니스 로직] 상태 변경 메서드
    public void hideComment() {
        this.status = "HIDDEN";
    }
    public void setStatus(String status) {
    this.status = status;
}
    
    public void updateContent(String content) {
        this.content = content;
    }

}