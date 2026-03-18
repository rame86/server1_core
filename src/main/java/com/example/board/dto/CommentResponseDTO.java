package com.example.board.dto;

import lombok.*;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // [수정] 가짜 builder() 메서드 대신 롬복 빌더를 적용합니다.
public class CommentResponseDTO {
    private Long commentId;     // 기존 id 필드를 commentId로 명확히 명명
    private Long boardId;       // [추가] 이 필드가 있어야 서비스 레이어의 에러가 해결됩니다.
    private Long memberId;
    private String content;
    private String status;
    private String authorName;
    private OffsetDateTime createdAt;

    // Entity -> DTO 변환 정적 메서드
    public static CommentResponseDTO from(com.example.board.entity.Comment comment) {
        return CommentResponseDTO.builder()
            .commentId(comment.getCommentId())
            // 엔티티의 Board boardId 필드에서 실제 Long ID를 추출하여 DTO에 전달
            .boardId(comment.getBoardId() != null ? comment.getBoardId().getBoardId() : null)
            .content(comment.getContent())
            .memberId(comment.getMemberId())
            .createdAt(comment.getCreatedAt())
            .status(comment.getStatus())
            .authorName("User_" + comment.getMemberId())
            .build();
    }
}