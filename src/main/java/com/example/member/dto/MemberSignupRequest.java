package com.example.member.dto;

import java.util.Map;

import lombok.Data;

@Data
public class MemberSignupRequest {
	private String email;
    private String name;
    private String phone;
    private String address;
    private String provider;
    private Map<String, Object> info; // 추가 정보들을 담을 맵
}
