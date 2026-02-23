package com.example.member.dto;

import java.util.Map;

import lombok.Data;

@Data
public class MemberSignupRequest {
	
    private String email;
    private String name;
    private String phone;
    private String address;
    private String age;
	private String password;

    private String provider;
    private String providerId;
    
    private String authCode;

    private Map<String, Object> info;
}
