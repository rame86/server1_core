package com.example.board.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDTO {
    private Long id;
    private String content;
    private Long memberId;
    private OffsetDateTime createdAt;
    private String status; // <--- [추가]

    // Entity -> DTO 변환 정적 메서드
    public static CommentResponseDTO from(com.example.board.entity.Comment comment) {
        return new CommentResponseDTO(
            comment.getCommentId(),
            comment.getContent(),
            comment.getMemberId(),
            comment.getCreatedAt(),
            comment.getStatus() // <--- [추가] 엔티티의 status 값을 DTO로 복사
        );
    }
}