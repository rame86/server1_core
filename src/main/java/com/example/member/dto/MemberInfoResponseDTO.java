package com.example.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberInfoResponseDTO {
    private String email;
    private String name;
    private String phone;
    private String age;
    private String address;
    private String profileImageUrl;
}
