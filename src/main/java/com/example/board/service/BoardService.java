package com.example.board.service;

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
    

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public List<BoardDTO> getBoardList(String category, Long memberId) {
        String searchCategory = (category == null || category.isEmpty() || "전체".equals(category)) ? "전체" : category;
        List<Board> boards = "전체".equals(searchCategory) ?
            boardRepository.findByStatusOrderByCreatedAtDesc("ACTIVE") : 
            boardRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, "ACTIVE");

        // 게시글 목록에 있는 모든 작성자 ID를 중복 없이 모읍니다.
        Set<Long> memberIds = boards.stream()
                .map(Board::getMemberId)
                .collect(Collectors.toSet());
         
        // 가져온 회원 정보를 Map<ID, 이름> 형태로 만듭니다.
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
        String originalFileName = null;
        String storedFileName = null;

        if (file != null && !file.isEmpty()) {
            originalFileName = file.getOriginalFilename();
            String fullPath = saveFile(file);
            storedFileName = new File(fullPath).getName();
        }
        Board board = Board.builder()
                .title(request.getTitle()).content(request.getContent())
                .category(request.getCategory()).memberId(memberId)
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
            deletePhysicalFile(uploadDir + File.separator + board.getStoredFilePath());
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
                deletePhysicalFile(uploadDir + File.separator + board.getStoredFilePath());
            }
            String originalFileName = file.getOriginalFilename();
            String fullPath = saveFile(file);
            board.updateFile(originalFileName, new File(fullPath).getName());
        } else if (Boolean.TRUE.equals(request.getFileDeleted())) {
            if (board.getStoredFilePath() != null) {
                deletePhysicalFile(uploadDir + File.separator + board.getStoredFilePath());
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
        File folder = new File(uploadDir);
        if (!folder.exists()) folder.mkdirs();
        String storedFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String storedFilePath = uploadDir + File.separator + storedFileName;
        file.transferTo(new File(storedFilePath));
        return storedFilePath;
    }

    private void deletePhysicalFile(String filePath) {
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) file.delete();
        }
    }

    private BoardDTO convertToDTO(Board board) {
        String fileUrl = board.getStoredFilePath() != null 
                ? "/msa/core/board/files/" + board.getStoredFilePath() 
                : null;
        return BoardDTO.builder()
                .boardId(board.getBoardId()).title(board.getTitle()).content(board.getContent())
                .category(board.getCategory()).memberId(board.getMemberId()).viewCount(board.getViewCount())
                .status(board.getStatus()).likeCount(board.getLikeCount()).commentCount(board.getCommentCount())
                .originalFileName(board.getOriginalFileName()).storedFilePath(fileUrl).artistPost(board.isArtistPost())
                .createdAt(board.getCreatedAt()).updatedAt(board.getUpdatedAt()).build();
    }
}