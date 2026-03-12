package com.example.admin.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class ReportBoardDTO {
    private Long reportId;
    private Long boardId;
    private Long memberId;
    private String reason;
    private LocalDateTime createdAt;
}