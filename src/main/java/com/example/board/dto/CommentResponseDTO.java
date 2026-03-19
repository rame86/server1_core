package com.example.board.dto;

import lombok.*;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDTO {
    private Long commentId;     // 기존 id 필드를 commentId로 명확히 명명
    private Long boardId;       // [추가] 이 필드가 있어야 서비스 레이어의 에러가 해결됩니다.
    private Long memberId;
    private String content;
    private String status;
    private String authorName;
    private OffsetDateTime createdAt;

    // 빌더 대신 사용하는 명시적 생성자 메서드
    public static CommentResponseDTO create(com.example.board.entity.Comment comment, String name) {
        return CommentResponseDTO.builder()
                .commentId(comment.getCommentId())
                .boardId(comment.getBoardId() != null ? comment.getBoardId().getBoardId() : null)
                .memberId(comment.getMemberId())
                .content(comment.getContent())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt())
                .authorName(name)
                .build();
    }
}