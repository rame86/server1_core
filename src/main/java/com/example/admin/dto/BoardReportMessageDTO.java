package com.example.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardReportMessageDTO {
    private Long boardId;    // 게시글 ID
    private Long commentId;  // 댓글 ID (새로 추가)
    private String status;   // 상태 (HIDDEN 등)

    // 편리한 사용을 위한 커스텀 생성자들
    public BoardReportMessageDTO(Long boardId) {
        this.boardId = boardId;
        this.status = "HIDDEN";
    }

    // 댓글용 객체 생성을 위한 정적 메서드
    public static BoardReportMessageDTO forComment(Long commentId) {
        return BoardReportMessageDTO.builder()
                .commentId(commentId)
                .status("HIDDEN")
                .build();
    }
}