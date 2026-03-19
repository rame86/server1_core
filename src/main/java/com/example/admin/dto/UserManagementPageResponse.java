package com.example.admin.dto;

import org.springframework.data.domain.Page;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserManagementPageResponse {
	private UserSummaryDTO summary;
	private Page<UserListResponseDTO> userList;
}
