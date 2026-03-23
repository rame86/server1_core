package com.example.board.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentReportRequestDTO {
    private Long commentId;  // 신고 대상 댓글 ID
    private Long memberId;   // 신고자(현재 로그인한 유저) ID
    private String reason;   // 신고 사유
}