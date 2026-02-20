package com.example.member.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
    private Long memberId; // 우리 시스템 내부 식별용 PK

	private String email;
	private String name;
	private String phone;
	private String address;
	private String age;
	private String password;
	
	// 아직은 용도가 불분명한 부가 정보들을 담는 용도로 jsonb 유지
	@Type(JsonBinaryType.class)
	@Column(columnDefinition = "jsonb")
	private Map<String, Object> info;

	// 한 명이 여러 소셜 계정을 가질 수 있음
	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SocialAccount> socialAccounts = new ArrayList<>();

}