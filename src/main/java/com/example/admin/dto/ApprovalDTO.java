package com.example.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface ApprovalDTO {
	@JsonProperty("eventId")
	Long getApprovalId();
	String getStatus();
	String getRejectionReason();
}
