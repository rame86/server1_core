package com.example.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserListResponseDTO {
	private Long memberId;
    private String name;
    private String email;
    private String createdAt;
    private String status;
    private Integer purchaseCount;
    private Long pointBalance;
}
