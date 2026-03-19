package com.example.board.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class BoardResponseDTO {
    private Long boardId;      
    private String status;     // API 응답 결과 (SUCCESS / FAIL)
    private String message;    
    private String type;       

    // 게시글 본연의 정보
    private String authorName;
    private boolean artistPost; // DB에 별도 컬럼이 있거나, 로직으로 계산된 값
}