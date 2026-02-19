package com.example.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
public class Member {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 우리 시스템 내부 식별용 PK

	@Column(unique = true)
    private String kakaoId;
	
	@Column(unique = true)
    private String naverId;
	
	@Column(unique = true)
	private String googleId;

    private String nickname;
    private String profileImageUrl;

    @Builder
    public Member(String kakaoId, String naverId, String googleId, String nickname, String profileImageUrl) {
        this.kakaoId = kakaoId;
        this.naverId = naverId;
        this.googleId = googleId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

}
