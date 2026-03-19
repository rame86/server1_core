package com.example.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSummaryDTO {
	private long totalUserCount;
    private long activeUserCount;
    private long blockedUserCount;
}
