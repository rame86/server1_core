package com.example.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor // [추가] 모든 필드를 인자로 받는 생성자
@Builder            // [추가] 테스트 및 서비스 로직 편의를 위한 빌더
@ToString           // [추가] 로그 확인용
public class BoardCreateRequest {
    private Long memberId;      // 작성자 식별자
    private String title;       // 게시글 제목
    private String content;     // 게시글 내용
    private String category;    // 카테고리
    private Long artistId;
    private Boolean FileDeleted; // 파일 삭제 여부
}