package com.example.board.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BoardCreateRequest {
    private Long memberId;      // 작성자 식별자
    private String title;       // 게시글 제목
    private String content;     // 게시글 내용
    private String category;    // 카테고리
}