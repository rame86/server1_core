package com.example.artist.entity;

import java.time.LocalDateTime;

import com.example.member.domain.Member;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "follow", schema = "artist",
	uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "artist_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Follow {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id")
    private Member member; // 팔로우 주체 (public 스키마의 member 테이블)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artist; // 팔로우 대상 (artist 스키마의 artist 테이블)
    
    private LocalDateTime createdAt;

}
