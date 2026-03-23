package com.example.board.entity;

import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "board", schema = "board")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardId;

    private String category;
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;

    private Long memberId;
    private Long artistId;   
    
    @Builder.Default
    private Integer viewCount = 0;
    @Builder.Default
    private Integer likeCount = 0;
    @Builder.Default
    private Integer commentCount = 0;

    private boolean isArtistPost;

    private String originalFileName;
    private String storedFilePath;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE"; // 기본값을 ACTIVE로 설정

    //status 필드를 "HIDDEN"으로 변경
    public void hideBoard() {
        this.status = "HIDDEN";
    }
    
    // --- view count ---
    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null) ? 1 : this.viewCount + 1;
    }
    
    public void incrementCommentCount() { // [추가] 명확한 증가 메서드
        if (this.commentCount == null) this.commentCount = 0;
        this.commentCount++;
    }

    public void decrementCommentCount() { // [추가] 명확한 감소 메서드
        if (this.commentCount == null || this.commentCount <= 0) this.commentCount = 0;
        else this.commentCount--;
    }

    // 게시글 정보 수정
    public void update(String title, String content, String category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }

    // 파일 정보 수정
    public void updateFile(String originalFileName, String storedFilePath) {
        this.originalFileName = originalFileName;
        this.storedFilePath = storedFilePath;
    }

    // 좋아요 수 변경 (증가/감소 공통 처리)
    public void updateLikeCount(boolean increment) {
        if (this.likeCount == null) this.likeCount = 0;
        this.likeCount = increment ? this.likeCount + 1 : Math.max(0, this.likeCount - 1);
    }

    // 댓글 수 변경 (증가/감소 공통 처리)
    public void updateCommentCount(boolean increment) {
        if (this.commentCount == null) this.commentCount = 0;
        this.commentCount = increment ? this.commentCount + 1 : Math.max(0, this.commentCount - 1);
    }

    public void setStatus(String string) {
        this.status = status;
    }

}