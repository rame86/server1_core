package com.example.member.entity;

import java.util.Map;

import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
	
	@Type(JsonBinaryType.class)
	@Column(columnDefinition = "jsonb")
	private Map<String, Object> info;

}
