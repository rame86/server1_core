package com.example.board.dto;

import lombok.*;
import java.time.OffsetDateTime;

@Getter 
@Setter 
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardDTO {
    private Long boardId;
    private String category;    
    private String title;
    private String content;
    
    // 작성자 식별자
    private Long memberId;      
    private String authorId;    // 필요한 경우 사용 (로그인 ID 등)
    
    // UI 노출용 실제 데이터 필드
    private String authorName;  // 실제 DB의 member.name이 담길 곳
    private String authorRole;  
    private String profileImg;  
    
    // 인터랙션 통계
    private int viewCount;
    private int likeCount;      
    private int commentCount;   
    
    // UI 제어용 필드 (Jackson이 boolean 필드를 처리할 때 artistPost로 매핑함)
    private boolean artistPost; 
    
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * Role 정보를 바탕으로 아티스트 여부를 자동으로 판단하는 편의 메서드
     */
    public void checkArtistStatus() {
        if ("ARTIST".equals(this.authorRole)) {
            this.artistPost = true;
        }
    }
}