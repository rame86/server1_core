package com.example.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ArtistApprovedEvent {
	private Long memberId; // 아티스트가 된 유저의 ID
    private String artistName; // 아티스트 활동명
    private String type;
}
