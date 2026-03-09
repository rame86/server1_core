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
    private Long approvalId;
    private Long eventId;
    private Long requesterId;
    private String status;
    private String eventTitle;
    private String rejectionReason;
    private String created_at;
}