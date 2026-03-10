package com.example.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class EventResultDTO {
    private Long approvalId; // 신청ID
    private Long requesterId; // 신청자ID
    private Long adminId;
    private String status;
    private String eventTitle;
    private String rejectionReason;
    private String createdAt;
}