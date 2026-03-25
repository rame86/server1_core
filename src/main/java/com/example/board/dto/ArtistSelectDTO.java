package com.example.board.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtistSelectDTO {
    private Long artistId;
    private String stageName;
}