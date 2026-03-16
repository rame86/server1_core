package com.example.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class EventResultDTO implements ApprovalDTO {
	@JsonAlias("approvalId")
	@JsonProperty("eventId")
    private Long approvalId; // eventID
	
	public void setApprovalId(Long approvalId) {
        this.approvalId = approvalId;
    }
	
    private Long requesterId; // 신청자ID
    private String status;
    private String eventTitle;
    private String rejectionReason;
    private String createdAt;
    private String eventStartDate; // 이벤트 시작일
    private String location; // 장소
    private Long price; // 금액
}