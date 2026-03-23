package com.example.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MemberUpdateRequestDTO {
    private String name;
    private String phone;
    private String nickName; // 컴파일 에러 원인: 누락되었던 닉네임 필드 추가
    private String age;
    private String address;
    private String profileImageUrl;
}
