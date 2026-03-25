package com.example.artist.dto;

import lombok.Data;

@Data
public class ArtistResponse {
	private Long memberId;
    private String stageName;
    private String description;
    private String communityLink;
    private long followerCount;
    private String category;
    private String profileImageUrl;
    private String fandomName;
    private String fandomImage;
}
