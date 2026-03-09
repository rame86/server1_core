package com.example.admin.dto;

public class EventResultDTO {
    private Long requesterId;      // 알림 받을 유저 ID
    private String status;         // APPROVED or REJECTED
    private String eventTitle;     // 스냅샷에서 꺼낸 이벤트 제목
    private String rejectionReason; // 거절 시 사유 (승인 시 null)
    private Long approvalId;       // (선택) 1서버에서 참조용으로 저장할 경우
}