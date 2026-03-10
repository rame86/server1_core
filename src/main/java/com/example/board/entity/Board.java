package com.example.board.entity;

import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="board", schema = "board")
@Getter
@Setter
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
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
    
    // 비즈니스 로직: 조회수 증가
    public void incrementViewCount() {
        if (this.viewCount == null) this.viewCount = 0;
        this.viewCount++;
    }

    // 비즈니스 로직: 게시글 수정
    public void update(String title, String content, String category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }
}