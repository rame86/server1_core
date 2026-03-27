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
     * [추가] 아티스트 레터 작성 (아티스트 본인이 작성)
     */
    @Transactional
    public void createArtistBoard(BoardDTO dto) {
        log.info("-----> [ArtistBoardService] 아티스트 레터 등록 시작: {}", dto.getTitle());
        
        // 1. 작성자가 아티스트인지 확인 및 실제 artistId 조회
        Artist artist = artistRepository.findByMemberId(dto.getMemberId());
        if (artist == null) {
            throw new IllegalArgumentException("아티스트 권한이 없는 사용자입니다.");
        }

        // 2. DTO -> Entity 변환 (artistId 설정 중요)
        Board board = Board.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .category("아티스트 레터") // 카테고리 고정
                .memberId(dto.getMemberId()) // 작성자 ID (MemberId)
                .artistId(artist.getArtistId()) // 소속 아티스트 ID
                .status("ACTIVE")
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .build();

        boardRepository.save(board);
        log.info("-----> [ArtistBoardService] 아티스트 레터 저장 완료: ID {}", board.getBoardId());
    }

    /**
     * [추가] 특정 아티스트가 작성한 '아티스트 레터' 목록 조회
     */
    @Transactional(readOnly = true)
    public List<BoardDTO> getArtistLetterList(Long memberId) {
        Artist artist = artistRepository.findByMemberId(memberId);
        if (artist == null) return Collections.emptyList();

        // 카테고리가 '아티스트 레터'인 것만 조회
        List<Board> letters = boardRepository.findByArtistIdAndCategory(artist.getArtistId(), "아티스트 레터");
        
        return letters.stream()
                .map(board -> convertToDTO(board, null))
                .collect(Collectors.toList());
    }

    /**
     * 특정 아티스트 페이지의 전체 팬 게시글 조회
     */
    @Transactional(readOnly = true)
    public List<BoardDTO> getBoardListByArtist(Long memberId, Long requesterId) {
        Artist artist = artistRepository.findByMemberId(memberId);
        if (artist == null) return Collections.emptyList();

        Long realArtistId = artist.getArtistId(); 
        log.info("-----> [ArtistBoardService] 아티스트 {}의 전체 게시글 조회", realArtistId);

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

        List<Board> notices = boardRepository.findByArtistIdAndCategory(artist.getArtistId(), "공지사항");
        return notices.stream()
                .map(board -> convertToDTO(board, null))
                .collect(Collectors.toList());
    }

    /**
     * 내부 사용용 DTO 변환 메서드
     */
    private BoardDTO convertToDTO(Board board, Long requesterId) {
        Member writer = memberRepository.findByMemberId(board.getMemberId());
        String authorName = (writer != null) ? writer.getName() : "탈퇴한 사용자";

        BoardDTO dto = BoardDTO.builder()
                .boardId(board.getBoardId())
                .title(board.getTitle())
                .content(board.getContent())
                .category(board.getCategory())
                .memberId(board.getMemberId())
                .authorName(authorName)
                .viewCount(board.getViewCount())
                .status(board.getStatus())
                .likeCount(board.getLikeCount())
                .commentCount(board.getCommentCount())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();

        // 로그인한 사용자의 좋아요 여부 체크
        if (requesterId != null) {
            dto.setLiked(likeRepository.existsByBoardIdAndMemberId(board.getBoardId(), requesterId));
        }
        return dto;
    }
}