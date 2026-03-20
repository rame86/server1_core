package com.example.admin.dto;

import java.time.LocalDateTime;

/**
 * [Admin -> Board] 메시지 전송을 위한 최소한의 데이터 그릇
 */
public record BoardReportMessageDTO(
    Long boardId,
    String status
) {
    // 가장 많이 쓰는 형태의 편의 생성자
    public BoardReportMessageDTO(Long boardId) {
        this(boardId, "HIDDEN");
    }
}