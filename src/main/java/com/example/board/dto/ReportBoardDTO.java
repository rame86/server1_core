package com.example.board.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReportBoardDTO {
    
    private Long reportId;     // 신고 고유 번호
    private Long boardId;      // 신고된 게시글 ID
    private Long memberId;     // 신고한 유저 ID
    private String reason;     // 신고 사유
    private String status;     // 상태 (PENDING, APPROVED, REJECTED)
    private LocalDateTime createdAt; // 신고 일시
    
}