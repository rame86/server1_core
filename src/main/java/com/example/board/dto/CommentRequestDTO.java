package com.example.board.dto;

import lombok.*;

@Getter
@Setter // [수정] 롬복 Setter를 추가하여 setBoardId, setMemberId를 자동 생성합니다.
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CommentRequestDTO {
    
    private Long boardId;   // [추가] 게시글 식별자
    private Long memberId;  // [추가] 작성자 식별자
    private String content; // 댓글 내용
}