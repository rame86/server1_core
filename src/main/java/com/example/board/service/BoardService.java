package com.example.board.service;

import com.example.board.dto.BoardCreateRequest;
import com.example.board.dto.BoardDTO;
import com.example.board.dto.BoardResponseDTO;
import com.example.board.entity.Board;
import com.example.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;

    @Value("${file.upload.dir}")
    private String uploadDir;

    // 게시글 전체 조회
    @Transactional(readOnly = true)
    public List<BoardDTO> getBoardList(String category) {
        String searchCategory = (category == null || category.isEmpty() || "전체".equals(category)) ? "전체" : category;
        List<Board> boards;
        if ("전체".equals(searchCategory)) {
            boards = boardRepository.findAllByOrderByCreatedAtDesc();
        } else {
            boards = boardRepository.findByCategoryOrderByCreatedAtDesc(searchCategory);
        }
        return boards.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // 게시글 상세 조회
    @Transactional
    public BoardDTO getBoardDetail(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. ID: " + id));
        board.incrementViewCount(); // 조회수 증가
        return convertToDTO(board);
    }

    // 게시글 작성
    @Transactional
    public BoardResponseDTO writeBoard(BoardCreateRequest request, MultipartFile file, Long memberId) throws IOException {
        String originalFileName = null;
        String storedFilePath = null;

        if (file != null && !file.isEmpty()) {
            File folder = new File(uploadDir);
            if (!folder.exists()) folder.mkdirs();

            originalFileName = file.getOriginalFilename();
            String storedFileName = UUID.randomUUID().toString() + "_" + originalFileName;
            storedFilePath = uploadDir + File.separator + storedFileName;
            file.transferTo(new File(storedFilePath));
        }

        Board board = Board.builder()
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .memberId(memberId)
                .originalFileName(originalFileName)
                .storedFilePath(storedFilePath)
                .build();

        Board savedBoard = boardRepository.save(board);

        // 결과 응답은 전용 DTO인 BoardResponseDTO 사용
        return BoardResponseDTO.builder()
                .boardId(savedBoard.getBoardId())
                .status("SUCCESS")
                .message("게시글 등록에 성공하였습니다.")
                .build();
    }
    
    // 게시글 삭제
    @Transactional
    public BoardResponseDTO deleteBoard(Long id, Long loginMemberId, String role) {
        // 게시글 존재여부 
        Board board = boardRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("삭제할 게시글이 존재하지 않습니다. ID: " + id));
        // 관리자이거나 작성자인 경우만 허용
        boolean isAdmin = "ADMIN".equals(role);
        boolean isAuthor = board.getMemberId().equals(loginMemberId);

        if (!isAdmin && !isAuthor) {
            log.warn("삭제 권한 없음: 요청자={}, 작성자={}", loginMemberId, board.getMemberId());
            throw new SecurityException("삭제 권한이 없습니다. 작성자 또는 관리자만 삭제 가능합니다.");
        }

        // 물리 파일 삭제
        if (board.getStoredFilePath() != null) {
            try {
                Path filePath = Paths.get(board.getStoredFilePath());
                Files.deleteIfExists(filePath);
                log.info("파일 삭제 완료: {}", board.getStoredFilePath());
            } catch (IOException e) {
                log.error("파일 삭제 중 오류 발생 (DB는 삭제 진행): {}", e.getMessage());
            }
        }

        boardRepository.delete(board);

        return BoardResponseDTO.builder()
                .boardId(id)
                .status("SUCCESS")
                .message("게시글이 성공적으로 삭제되었습니다.")
                .build();
    }

    // 게시글 수정
    @Transactional
    public BoardResponseDTO updateBoard(
        Long id, BoardCreateRequest request, MultipartFile file, Long loginMemberId, String role) throws IOException {
        // 1. 기존 게시글 및 권환 확인
        Board board = boardRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 권한 체크: 작성자 본인 혹은 관리자만 가능
        boolean isAdmin = "ADMIN".equals(role);
        boolean isAuthor = board.getMemberId().equals(loginMemberId);
            
        if (!isAdmin && !isAuthor){
            throw new SecurityException("수정 권한이 없습니다.");
        }
        // 2. 기존 정보 수정 (제목, 내용, 카테고리)
        board.setTitle(request.getTitle());
        board.setContent(request.getContent());
        board.setCategory(request.getCategory());

        // 3. 파일 처리 로직
        // 새 파일이 전송된 경우만 기존 파일 지우고 새로등록
        if(file !=null && !file.isEmpty()){
            log.info("새 파일 감지: 기존 파일 삭제 및 교체 진행");

            //[기존 파일 삭제] 서버에 실제 파일이 있다면 삭제
            if(board.getStoredFilePath() !=null){
                try{
                    Path oldFilePath = Paths.get(board.getStoredFilePath());
                    Files.deleteIfExists(oldFilePath);
                }catch (IOException e){
                    log.error("기존 파일 삭제 실패:{}", e.getMessage());
                }
            }
        // [새 파일 저장]
        String originalFileName = file.getOriginalFilename();
        String storedFileName = UUID.randomUUID().toString() + "_" + originalFileName;
        String savePath = uploadDir + storedFileName;

        file.transferTo(new File(savePath));

        // [DB 정보 갱신]
        board.setOriginalFileName(originalFileName);
        board.setStoredFilePath(savePath);
        }else {
            log.info("새 파일 없음: 기존 파일 정보 유지");
        }
        return BoardResponseDTO.builder()
                .boardId(id)
                .status("SUCCESS")
                .message("게시글이 성공적으로 수정되었습니다.")
                .build();
    }


    // Entity를 BoardDTO로 변환하는 공통 메서드
    private BoardDTO convertToDTO(Board board) {
        return BoardDTO.builder()
                .boardId(board.getBoardId())
                .category(board.getCategory())
                .title(board.getTitle())
                .content(board.getContent())
                .memberId(board.getMemberId())
                .viewCount(board.getViewCount())
                .originalFileName(board.getOriginalFileName())
                .storedFilePath(board.getStoredFilePath())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }
}