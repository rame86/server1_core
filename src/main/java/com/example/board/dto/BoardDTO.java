package com.example.board.dto; // 설정된 패키지 경로 준수

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
public class BoardDTO {
    private Long boardId;
    private String category;    // [추가] 팬레터, 아티스트 레터, 공지 등
    private String title;
    private String content;
    private String authorId;
   
    // UI 노출용 추가 필드
    private Long memberId;        // DB의 bigint와 매핑
    private String authorName;    // member.name (예: 김지수, 이하은)
    private String authorRole;    // member.role (예: USER, ARTIST, ADMIN)
    private String profileImg;    // member.info(jsonb) 내 경로 혹은 별도 컬럼 (현재는 가상 필드)
    
    // 3. 인터랙션 및 통계
    private int viewCount;
    private int likeCount;      // [추가] 하트 수
    private int commentCount;   // [추가] 말풍선 수
    
    // 4. UI 제어용 상태 필드
    private boolean isArtistPost; // authorRole이 'ARTIST'일 경우 true로 설정하는 로직 권장
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}