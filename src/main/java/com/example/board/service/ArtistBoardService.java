package com.example.board.service;

import com.example.artist.entity.Artist;
import com.example.artist.repository.ArtistRepository;
import com.example.board.dto.BoardDTO;
import com.example.board.entity.Board;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.LikeRepository;
import com.example.member.domain.Member;
import com.example.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistBoardService {

    private final BoardRepository boardRepository;
    private final ArtistRepository artistRepository;
    private final LikeRepository likeRepository;
    private final MemberRepository memberRepository;

    /**
     * 특정 아티스트(MemberId 기준)의 전체 게시글 조회
     */
    @Transactional(readOnly = true)
    public List<BoardDTO> getBoardListByArtist(Long memberId, Long requesterId) {
        Artist artist = artistRepository.findByMemberId(memberId);
        if (artist == null) return Collections.emptyList();

        // 2. 해당 아티스트의 고유 번호인 artist_id(3)를 가져옴
        Long realArtistId = artist.getArtistId(); 
        log.info("-----> [ArtistBoardService] 멤버 {}의 실제 아티스트ID {} 조회", memberId, realArtistId);

        // 3. board 테이블에서 artist_id가 3인 글들을 조회
        List<Board> boards = boardRepository.findByArtistId(realArtistId);
        return boards.stream()
                    .map(board -> convertToDTO(board, requesterId))
                    .collect(Collectors.toList());
}

    /**
     * 특정 아티스트의 공지사항 조회
     */
    @Transactional(readOnly = true)
    public List<BoardDTO> getNoticeListByArtist(Long memberId) {
       Artist artist = artistRepository.findByMemberId(memberId);
        if (artist == null) return Collections.emptyList();

        // 공지사항도 artist_id로 조회
        List<Board> notices = boardRepository.findByArtistIdAndCategory(artist.getArtistId(), "공지사항");
        return notices.stream()
                    .map(board -> convertToDTO(board, null))
                    .collect(Collectors.toList());
    }

    /**
     * 내부 사용용 DTO 변환 메서드 (BoardService의 것과 동일하게 구성)
     */
    private BoardDTO convertToDTO(Board board, Long requesterId) {
        Member writer = memberRepository.findByMemberId(board.getMemberId());
        String authorName = (writer != null) ? writer.getName() : "탈퇴한 사용자";

        BoardDTO dto = BoardDTO.builder()
                .boardId(board.getBoardId()).title(board.getTitle()).content(board.getContent())
                .category(board.getCategory()).memberId(board.getMemberId()).authorName(authorName)
                .viewCount(board.getViewCount()).status(board.getStatus()).likeCount(board.getLikeCount())
                .commentCount(board.getCommentCount())
                .createdAt(board.getCreatedAt()).updatedAt(board.getUpdatedAt()).build();

        if (requesterId != null) {
            dto.setLiked(likeRepository.existsByBoardIdAndMemberId(board.getBoardId(), requesterId));
        }
        return dto;
    }
}