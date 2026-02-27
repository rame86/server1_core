package com.example.board.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="board", schema = "board")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 빈 생성자 외부로부터 보호
@AllArgsConstructor // 모든 필드를 다 때려 넣는 생성자
@Builder
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardId; // int8 -> Long

    private String category; // varchar -> String
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;

    private Long memberId; // int8 -> Long

    private Integer likeCount; // int4 -> Integer
    private Integer commentCount;
    private Integer viewCount;

    private boolean isArtistPost; // bool -> boolean

    @CreationTimestamp // insert 시 자동 시간 입력
    private OffsetDateTime createdAt; // timestamptz -> OffsetDateTime

    @UpdateTimestamp // update 시 자동 시간 입력
    private OffsetDateTime updatedAt;
}
