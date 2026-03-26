package com.example.board.controller;

import com.example.board.service.BoardCommentService;
import com.example.board.service.BoardReportService;
import com.example.board.service.BoardService;
import com.example.admin.dto.ReportBoardDTO;
import com.example.board.dto.*;
import com.example.common.annotation.LoginUser;
import com.example.member.dto.RedisMemberDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final BoardCommentService boardCommentService;
    private final BoardReportService boardReportService;

    @Value("${file.upload.dir}")
    private String uploadDir;

    // 팬덤, 아티스트 목록
    @GetMapping("/my-fandoms")
    public ResponseEntity<List<ArtistSelectDTO>> getMyFandoms(@LoginUser RedisMemberDTO loginUser) {
    // 1. 로그인 체크
    if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    
    log.info("====> [가입 팬덤 조회] memberId: {}", loginUser.getMemberId());
    
    // 2. 서비스 호출 (DTO 리스트 반환)
    List<ArtistSelectDTO> fandomList = boardService.getMyFandomList(loginUser.getMemberId());
    
    return ResponseEntity.ok(fandomList);
}

    // --- [1. 게시글 관련 API] ---
    @GetMapping("/list")
    public ResponseEntity<List<BoardDTO>> getList(
        @LoginUser RedisMemberDTO loginUser,
        @RequestParam(name = "category", required = false) String category) {
        log.info("====> [목록 조회] 카테고리: {}", category);
        List<BoardDTO> list = boardService.getBoardList(category, loginUser != null ? loginUser.getMemberId() : null);
        return ResponseEntity.ok(list);
    }

    // 게시글 보기
    @GetMapping("/{id}")
    public ResponseEntity<BoardDTO> getDetail(
        @LoginUser RedisMemberDTO loginUser, 
        @PathVariable(name = "id") Long id) {
        BoardDTO detail = boardService.getBoardDetail(id, loginUser != null ? loginUser.getMemberId() : null);
        return ResponseEntity.ok(detail);
    }
     // 게시글 작성
    @PostMapping(value = "/write", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<BoardResponseDTO> write(
            @LoginUser RedisMemberDTO loginUser,
            @RequestPart("request") BoardCreateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        BoardResponseDTO response = boardService.writeBoard(request, file, loginUser.getMemberId());
        return ResponseEntity.ok(response);
    }
    
    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<BoardResponseDTO> update(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "id") Long id,
            @RequestPart("request") BoardCreateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) throws Exception {

        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        BoardResponseDTO response = boardService.updateBoard(id, request, file, loginUser.getMemberId(), loginUser.getRole());
        return ResponseEntity.ok(response);
    }
    // 게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<BoardResponseDTO> delete(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "id") Long id) {

        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        BoardResponseDTO response = boardService.deleteBoard(id, loginUser.getMemberId(), loginUser.getRole());
        return ResponseEntity.ok(response);
    }
    // 게시글 좋아요
    @PostMapping("/{id}/like")
    public ResponseEntity<Integer> toggleLike(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "id") Long id) {

        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        int updatedLikeCount = boardService.toggleLike(id, loginUser.getMemberId());
        return ResponseEntity.ok(updatedLikeCount);
    }

    // --- [2. 댓글 관련 API] ---
    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponseDTO> addComment(
            @PathVariable(name = "id") Long boardId,
            @LoginUser RedisMemberDTO loginUser,
            @RequestBody CommentRequestDTO request) {

        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        request.setBoardId(boardId);
        request.setMemberId(loginUser.getMemberId());
        
        CommentResponseDTO response = boardCommentService.createComment(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponseDTO>> getComments(@PathVariable(name = "id") Long id) {
        List<CommentResponseDTO> comments = boardCommentService.getCommentsByBoardId(id);
        return ResponseEntity.ok(comments);
    }

   @PutMapping("/comments/{commentId}")
   public ResponseEntity<CommentResponseDTO> updateComment(
        @LoginUser RedisMemberDTO loginUser,
        @PathVariable(name = "commentId") Long commentId,
        @RequestBody CommentRequestDTO requestDTO){

        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        CommentResponseDTO response = boardCommentService.updateComment(commentId,requestDTO,loginUser.getMemberId());
        return ResponseEntity.ok(response);
    }
   
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
        @LoginUser RedisMemberDTO loginUser,
        @PathVariable(name = "commentId") Long commentId) {
            
        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        boardCommentService.deleteComment(commentId, loginUser.getMemberId(), loginUser.getRole());
        return ResponseEntity.noContent().build();
    }
    
    // --- [파일 서빙 API (미리보기 및 다운로드 통합)] ---
    @GetMapping("/files/{*fileName}")
    public ResponseEntity<Resource> serveFile(@PathVariable(name = "fileName") String fileName) {
        try {
        // 1. URL 인코딩 해제 (한글 파일명 대비)
        String decodedPath = java.net.URLDecoder.decode(fileName, StandardCharsets.UTF_8);
        
        // 2. 경로에서 순수 파일명만 추출 (앞에 '/'나 'board/'가 붙어와도 파일명만 쏙 뺍니다)
        String pureFileName = decodedPath.contains("/") 
            ? decodedPath.substring(decodedPath.lastIndexOf("/") + 1) 
            : decodedPath;

        // 3. [핵심] 어떤 환경이든 uploadDir(core)를 기준으로 'board' 폴더 안의 파일명을 찾습니다.
        // resolve("board")를 통해 C:/msa_uploads/board/ 또는 /app/.../core/board/ 경로가 완성됩니다.
        Path filePath = Paths.get(uploadDir).resolve("board").resolve(pureFileName).normalize();
        File file = filePath.toFile();

        // 디버깅 로그 (서버 터미널에서 실제 어디를 뒤지는지 확인용)
        log.info("-----> [파일 탐색 경로 확인]: {}", file.getAbsolutePath());

        if (file.exists() && file.isFile()) {
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + 
                            UriUtils.encode(pureFileName, StandardCharsets.UTF_8) + "\"")
                    .body(resource);
        } else {
            log.warn("-----> [파일 없음]: {}", pureFileName);
            return ResponseEntity.notFound().build();
        }
    } catch (Exception e) {
        log.error("-----> [파일 서빙 에러]: ", e);
        return ResponseEntity.internalServerError().build();
    }
    }

    // --- [4. 사용자용 신고 접수 API] ---
    @PostMapping("/{id}/report/submit")
    public ResponseEntity<Void> reportBoard(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "id") Long boardId,
            @RequestBody ReportBoardDTO request) {
        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boardReportService.reportBoard(boardId, loginUser.getMemberId(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/comments/{commentId}/report/submit")
    public ResponseEntity<Void> reportComment(
            @LoginUser RedisMemberDTO loginUser,
            @PathVariable(name = "commentId") Long commentId,
            @RequestBody ReportBoardDTO request) {
        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boardReportService.reportComment(commentId, loginUser.getMemberId(), request.getReason());
        return ResponseEntity.ok().build();
    }
}