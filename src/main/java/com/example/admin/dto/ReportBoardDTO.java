package com.example.admin.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class ReportBoardDTO {
    private Long reportId;      // 신고 고유 번호
    private Long boardId;       // 신고된 게시글 ID
    private String postTitle;   // 신고된 게시글의 제목
    private String content;     // ★ [추가] 이 필드가 있어야 Board 서비스에서 보낸 본문을 받을 수 있습니다!
    private Long memberId;      // 신고한 유저 ID
    private String reason;      // 신고 사유
    private String status;      // 상태 (PENDING, APPROVED, REJECTED)
    private LocalDateTime createdAt; // 신고 일시
    
}