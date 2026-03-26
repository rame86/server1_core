package com.example.board.service;

import com.example.artist.entity.Artist;
import com.example.artist.entity.Follow;
import com.example.artist.repository.FollowRepository;
import com.example.board.dto.ArtistSelectDTO;
import com.example.board.dto.BoardCreateRequest;
import com.example.board.dto.BoardDTO;
import com.example.board.dto.BoardResponseDTO;
import com.example.board.entity.Board;
import com.example.board.entity.LikeBoard;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.LikeRepository;
import com.example.board.repository.CommentRepository;
import com.example.member.domain.Member;
import com.example.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final LikeRepository likeRepository;
    private final BoardRepository boardRepository; 
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final FollowRepository followRepository;
    

    @Value("${file.upload.dir}")
    private String uploadDir;

    // 내가 가입한 팬덤,아티스트 리스트 조회
    @Transactional(readOnly = true)
    public List<ArtistSelectDTO> getMyFandomList(Long memberId) {
        // 1. Member 객체 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 팔로우 리스트 조회
        List<Follow> follows = followRepository.findAllByMember(member);
        log.info("====> [Service] 팔로우 데이터 조회 완료, 개수: {}", follows.size());

        // 3. DTO 변환 (Artist 엔티티의 필드 그대로 사용)
        return follows.stream()
                .map(follow -> {
                    Artist artist = follow.getArtist();
                    return ArtistSelectDTO.builder()
                            .artistId(artist.getArtistId()) // Artist 엔티티의 artistId 필드 사용
                            .stageName(artist.getStageName() != null ? artist.getStageName() : artist.getMember().getName())
                            .build();
                })
                .collect(Collectors.toList());
    }
 // 게시판 목록 조회   
@Transactional(readOnly = true)
    public List<BoardDTO> getBoardList(String category, Long memberId) {
        String searchCategory = (category == null || category.isEmpty() || "전체".equals(category)) ? "전체" : category;
        List<Board> boards = "전체".equals(searchCategory) ?
            boardRepository.findByStatusOrderByCreatedAtDesc("ACTIVE") : 
            boardRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, "ACTIVE");

        // 작성자 ID 목록 추출
        Set<Long> memberIds = boards.stream()
                .map(Board::getMemberId)
                .collect(Collectors.toSet());
         
        // 회원 정보 Map 생성 (Batch 조회 최적화)
        Map<Long, String> memberNameMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(
                    Member::getMemberId, 
                    Member::getName, 
                    (existing, replacement) -> existing
                ));
       return boards.stream().map(board -> {
           BoardDTO dto = convertToDTO(board);
            dto.setAuthorName(memberNameMap.getOrDefault(board.getMemberId(), "탈퇴한 사용자"));
            
            if (memberId != null) {
                dto.setLiked(likeRepository.existsByBoardIdAndMemberId(board.getBoardId(), memberId));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    // [게시글 상세 조회] 조회수 증가 + 좋아요 여부 확인
    @Transactional
    public BoardDTO getBoardDetail(Long boardId, Long memberId) {
        // 1. DB에서 게시글 조회
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        board.incrementViewCount();

        BoardDTO dto = convertToDTO(board);

        // [상세조회] 작성자 이름 DB 조회 후 세팅
        Member writer = memberRepository.findByMemberId(board.getMemberId());
        if (writer != null) {
            dto.setAuthorName(writer.getName());
        } else {
            dto.setAuthorName("알 수 없는 사용자");
        }
        if (memberId != null) {
            boolean isLiked = likeRepository.existsByBoardIdAndMemberId(boardId, memberId);
            dto.setLiked(isLiked);
        }
        return dto;
    }

    // 게시글 작성
    @Transactional
    public BoardResponseDTO writeBoard(BoardCreateRequest request, MultipartFile file, Long memberId) throws IOException {
        // request.getArtistId()가 있다면 (팬레터, 팬덤게시판 등) 팔로우 여부 확인
        if (request.getArtistId() != null) {
            // followerId(memberId)가 artistId를 팔로우하는지 체크
           boolean isFollowing = followRepository.existsByMember_MemberIdAndArtist_ArtistId(memberId, request.getArtistId());
            if (!isFollowing) {
                log.warn("권한 없음: memberId {}는 artistId {}를 팔로우하지 않음", memberId, request.getArtistId());
                throw new IllegalStateException("해당 아티스트를 팔로우해야 글을 작성할 수 있습니다.");
            }
        }
        String originalFileName = null;
        String storedFileName = null;

        if (file != null && !file.isEmpty()) {
            originalFileName = file.getOriginalFilename();
            // 수정: 저장된 파일명만 반환받도록 변경
            storedFileName = saveFile(file); 
        }
        Board board = Board.builder()
                .title(request.getTitle()).content(request.getContent())
                .category(request.getCategory()).memberId(memberId).artistId(request.getArtistId())
                .originalFileName(originalFileName).storedFilePath(storedFileName).status("ACTIVE").build();
        boardRepository.save(board);
        return BoardResponseDTO.builder().boardId(board.getBoardId()).status("SUCCESS").build();
    }
    // 게시글 삭제
    @Transactional
    public BoardResponseDTO deleteBoard(Long id, Long memberId, String role) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 게시글이 없습니다."));

        if (!board.getMemberId().equals(memberId) && !"ADMIN".equals(role)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        // 연관 데이터 삭제 (순서 중요)
        commentRepository.deleteByBoardId_BoardId(id); // 댓글 먼저 삭제
        likeRepository.deleteByBoardId(id);    // 좋아요 삭제
        // 파일 삭제
        if (board.getStoredFilePath() != null) {
            deletePhysicalFile(board.getStoredFilePath());
        }
        boardRepository.delete(board);
        return BoardResponseDTO.builder().boardId(id).status("SUCCESS").message("삭제되었습니다.").build();
    }

    // [중요] 게시글 숨김 처리 (신고 승인 시 호출용)
    @Transactional
    public void hideBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다. ID: " + boardId));
        board.hideBoard(); // status = "HIDDEN" 변경 로직
        log.info("-----> [BoardService] 게시글 ID {} 가 숨김 처리되었습니다.", boardId);
    }

    // 게시글 수정
    @Transactional
    public BoardResponseDTO updateBoard(Long id, BoardCreateRequest request, MultipartFile file, Long memberId, String role) throws IOException {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("수정할 게시글이 없습니다."));

        if (!board.getMemberId().equals(memberId) && !"ADMIN".equals(role)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }
        
        board.update(request.getTitle(), request.getContent(), request.getCategory());
       
        if (file != null && !file.isEmpty()) {
            if (board.getStoredFilePath() != null) {
                deletePhysicalFile(board.getStoredFilePath());
            }
            String originalFileName = file.getOriginalFilename();
            String storedFileName = saveFile(file); 
            board.updateFile(originalFileName, storedFileName); // 파일수정
        } else if (Boolean.TRUE.equals(request.getFileDeleted())) {
            if (board.getStoredFilePath() != null) {
                deletePhysicalFile(board.getStoredFilePath());
            }
            board.updateFile(null, null);
        }
        return BoardResponseDTO.builder().boardId(id).status("SUCCESS").message("수정되었습니다.").build();
    }
    // 좋아요 토글
    @Transactional
    public int toggleLike(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        
        // 1. 이미 좋아요를 눌렀는지 확인
        boolean exists = likeRepository.existsByBoardIdAndMemberId(boardId, memberId);

        if (exists) {
            // 2. 이미 있다면 삭제 (좋아요 취소)
            likeRepository.deleteByBoardIdAndMemberId(boardId, memberId);
            board.updateLikeCount(false);
        } else {
            // 3. 없다면 추가 (좋아요) 중복 클릭 방지를 위해 한 번 더 체크하거나 try-catch로 DB 예외 처리 가능
            try {
                likeRepository.save(LikeBoard.builder().boardId(boardId).memberId(memberId).build());
                board.updateLikeCount(true);
            } catch (Exception e) {
                log.warn("이미 좋아요 처리가 진행 중입니다.");
            }
        }// 변경된 count 반영
        boardRepository.save(board); 
        return board.getLikeCount();
    }

    // 파일 관련 보조 메서드
   private String saveFile(MultipartFile file) throws IOException {
    // 설정된 경로(core) 밑에 'board' 폴더를 코드가 직접 붙입니다.
    Path uploadPath = Paths.get(uploadDir).resolve("board").normalize();
    
    try {
        // board 폴더가 없으면 자동으로 생성
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath); 
            log.info("-----> [폴더 생성] 경로에 board 폴더가 없어 새로 생성했습니다: {}", uploadPath);
        }

        // 2. 파일명 생성 및 저장
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        file.transferTo(filePath.toFile());

        log.info("-----> [파일 저장 성공] 물리 경로: {}", filePath);

        // DB에는 'board/파일명' 형태로 저장
        return "board/" + fileName;
        
    } catch (IOException e) {
        throw new RuntimeException("파일 저장 실패", e);
    }
}

    private void deletePhysicalFile(String fileName) {
        if (fileName != null) {
            Path filePath = Paths.get(uploadDir).resolve(fileName).toAbsolutePath().normalize();
            File file = filePath.toFile();
            if (file.exists()) {
                file.delete();
                log.info("물리 파일 삭제 완료: {}", filePath);
            }
        }
    }
    private BoardDTO convertToDTO(Board board) {
    // 기존의 "/images/core/"를 제거하고 DB에 저장된 실제 파일명(UUID_파일명)만 보냅니다.
    String fileName = board.getStoredFilePath();
    
        return BoardDTO.builder()
                .boardId(board.getBoardId()).title(board.getTitle()).content(board.getContent())
                .category(board.getCategory()).memberId(board.getMemberId()).viewCount(board.getViewCount())
                .status(board.getStatus()).likeCount(board.getLikeCount()).commentCount(board.getCommentCount())
                .originalFileName(board.getOriginalFileName()).storedFilePath(board.getStoredFilePath()).artistPost(board.isArtistPost())
                .createdAt(board.getCreatedAt()).updatedAt(board.getUpdatedAt()).build();
    }
}