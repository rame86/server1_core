package com.example.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListResponseDTO {
	private Long memberId;
    private String name;
    private String email;
    private String createdAt;
    private String status;
    private Integer purchaseCount;
    
    @JsonProperty("balance")
    private Long pointBalance;
}
