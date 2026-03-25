package com.example.artist.entity;

import com.example.member.domain.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "artist", schema = "artist")
@Getter
@Setter
@NoArgsConstructor
public class Artist {

    @Id
    private Long memberId;

    @MapsId // Artist의 PK를 Member의 PK와 매핑
    @OneToOne(fetch = FetchType.LAZY) // 지연 로딩으로 성능 최적화
    @JoinColumn(name = "member_id")
    private Member member;

    private String stageName; // 활동명
    private String communityLink; // 아티스트별 커뮤니티 링크 (외부 링크)
    private String description; // 소개글
    private String category;
    private String profileImageUrl;
    private int followerCount = 0;
    private String fandomName;


    // Artist.java
    @Column(name = "artist_id") // DB의 artist_id 컬럼과 매칭
    private Long artistId;

}
