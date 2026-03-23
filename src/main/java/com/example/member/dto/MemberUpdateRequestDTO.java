package com.example.member.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberUpdateRequestDTO {
    private String name;
    private String phone;
    private String age;
    private String address;
    private String profileImageUrl;
}
