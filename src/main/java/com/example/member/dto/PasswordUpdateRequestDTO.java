package com.example.member.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordUpdateRequestDTO {
    private String currentPassword;
    private String newPassword;
}
