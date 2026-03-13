package com.example.admin.dto;

import java.time.LocalDateTime;

public record BoardReportMessageDTO (
	Long boardId,
    String boardTitle,
    Long memberId,
    String nickname,
    String category, // 신고 사유(욕설, 비방, 스팸 등등)
    String content, // 신고 상세 내용
    String status, // 상태(PENDING, COMPLETED 등)
    LocalDateTime reportedAt // 신고일
) {
    // 이 생성자를 추가하면 AdminService2의 코드를 수정할 필요가 없습니다.
    public BoardReportMessageDTO(Long boardId, String status) {
        this(boardId, null, null, null, null, null, status, LocalDateTime.now());
    }
}
