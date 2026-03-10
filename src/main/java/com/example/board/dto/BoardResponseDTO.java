package com.example.board.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class BoardResponseDTO {
    private Long boardId;      // 처리된 게시글 ID
    private String status;     // SUCCESS, FAIL
    private String message;    // 결과 메시지
    private String type;       // 요청의 타입 복사
    
    // 필요한 경우 추가적인 UI 노출용 필드
    private String authorName;
    private boolean artistPost;
}