package com.example.artist.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.admin.dto.approval.ArtistResultDTO;
import com.example.artist.dto.ArtistResponse;
import com.example.artist.dto.DonationRequset;
import com.example.artist.service.ArtistService;
import com.example.board.dto.BoardDTO;
import com.example.board.service.ArtistBoardService;
import com.example.common.annotation.LoginUser;
import com.example.common.service.FileUploadService;
import com.example.member.dto.RedisMemberDTO;
import com.example.member.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/artist")
@RequiredArgsConstructor
public class ArtistController {
    
    private final ArtistService artistService;
    private final ArtistBoardService artistBoardService;
    private final MemberService memberService;
    private final FileUploadService fileUploadService;

    // 1. 아티스트 기본 정보 조회
    @GetMapping("/list")
    public ResponseEntity<List<ArtistResponse>> getList() {
        return ResponseEntity.ok(artistService.getAllArtist());
    }

    @GetMapping("/my-follows")
    public ResponseEntity<List<ArtistResponse>> getMyFollows(@LoginUser RedisMemberDTO user) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(artistService.getFollowedArtists(user.getMemberId()));
    }
    
    @PostMapping("/follow/{artistId}")
    public ResponseEntity<String> followArtist(
            @LoginUser RedisMemberDTO user,
            @PathVariable("artistId") Long artistId) {
        if (user == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        return ResponseEntity.ok(artistService.toggleFollow(user.getMemberId(), artistId));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ArtistResponse> getArtistDetail(@PathVariable("id") Long id) {
        log.info("-----> [ARTIST DETAIL REQUEST] 아티스트ID: {}", id);
        ArtistResponse artist = artistService.getArtistById(id);
        return ResponseEntity.ok(artist);
    }

    // 2. 게시글 및 레터 관련 API

    /**
     * 아티스트 레터 작성 (POST)
     */
    @PostMapping("/artist-letter")
    public ResponseEntity<?> createArtistLetter(
        @LoginUser RedisMemberDTO user, 
        @RequestBody BoardDTO dto) {
    
    if (user == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
    
    dto.setMemberId(user.getMemberId());
    // dto.setType("artist-letter"); <-- 이 줄을 삭제하거나 아래처럼 변경
    dto.setArtistPost(true); // 아티스트가 작성하는 글임을 명시
    dto.setCategory("아티스트 레터"); // 카테고리로 구분
    
    artistBoardService.createArtistBoard(dto);
    return ResponseEntity.ok(Map.of("message", "아티스트 레터가 성공적으로 등록되었습니다! ✨"));
}

    /**
     * 아티스트별 아티스트 레터 목록 조회 (GET)
     */
    @GetMapping("/{id}/artist-letters")
    public ResponseEntity<?> getArtistLetters(@PathVariable("id") Long id) {
        log.info("-----> [ARTIST-LETTERS REQUEST] 아티스트ID: {}", id);
        List<BoardDTO> letters = artistBoardService.getArtistLetterList(id); 
        return ResponseEntity.ok(letters);
    }

    /**
     * 아티스트별 팬레터(자유게시판) 목록 조회
     */
    @GetMapping("/{id}/fan-letters")
    public ResponseEntity<?> getArtistFanLetters(
            @LoginUser RedisMemberDTO user,
            @PathVariable("id") Long id) {
        Long requesterId = (user != null) ? user.getMemberId() : null;
        log.info("-----> [FAN-LETTERS REQUEST] 아티스트ID: {}", id);
        List<BoardDTO> list = artistBoardService.getBoardListByArtist(id, requesterId); 
        return ResponseEntity.ok(list);
    }

    /**
     * 아티스트별 공지사항 조회
     */
    @GetMapping("/{id}/notices")
    public ResponseEntity<?> getArtistNotices(@PathVariable("id") Long id) {
        log.info("-----> [NOTICES REQUEST] 아티스트ID: {}", id);
        List<BoardDTO> notices = artistBoardService.getNoticeListByArtist(id);
        return ResponseEntity.ok(notices);
    }

    // 3. 후원 및 신청 관련
    @PostMapping("/donate")
    public ResponseEntity<String> donate(@LoginUser RedisMemberDTO user, @RequestBody DonationRequset req) {
        if (user == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        
        log.info("-----> [DONATE REQUEST] 요청자: {}, 아티스트: {}, 금액: {}", user.getMemberId(), req.getArtistId(), req.getAmount());
        
        if(BigDecimal.valueOf(user.getBalance()).compareTo(req.getAmount()) < 0) {
            return ResponseEntity.badRequest().body("잔액이 부족합니다.");
        }
        
        String orderId = artistService.donateToArtist(user.getMemberId(), req.getArtistId(), req.getAmount());
        return ResponseEntity.ok("후원 요청 완료! 주문번호: " + orderId);
    }
    
    @PostMapping("/apply")
    public ResponseEntity<?> artistApply(@LoginUser RedisMemberDTO user, @RequestBody ArtistResultDTO dto) {
        if(user == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요한 서비스입니다."));
        try {
            memberService.applyArtist(user.getMemberId(), dto);
            log.info("-----> [아티스트 신청 접수] 요청자 ID: {}", user.getMemberId());
            return ResponseEntity.ok(Map.of("message", "아티스트 신청이 성공적으로 접수되었습니다."));         
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("아티스트 신청 중 서버 오류 발생: ", e);
            return ResponseEntity.status(500).body(Map.of("message", "시스템 오류가 발생했습니다. 다시 시도해주세요."));
        }
    }

    // 4. 팬덤 브랜딩 관련
    @PostMapping("/bg-image")
    public ResponseEntity<?> uploadArtistBgImage(@LoginUser RedisMemberDTO user, @RequestParam("bgImageFile") MultipartFile file) {
        if (user == null) return ResponseEntity.status(401).build();
        try {
            String url = fileUploadService.uploadImage(file, "artist");
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("아티스트 이미지 업로드 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "이미지 업로드에 실패했습니다."));
        }
    }

    @PostMapping("/update-fandom")
    public ResponseEntity<?> updateFandom(@LoginUser RedisMemberDTO user, @RequestBody ArtistResultDTO dto) {
        if(user == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요한 서비스입니다."));
        
        try {
            artistService.updateFandomInfo(user.getMemberId(), dto);
            log.info("-----> [팬덤 브랜딩 수정 완료] 아티스트 ID: {}", user.getMemberId());
            return ResponseEntity.ok(Map.of("message", "팬덤 브랜딩이 성공적으로 수정되었어! ✨"));         
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("팬덤 브랜딩 수정 중 서버 오류 발생: ", e);
            return ResponseEntity.status(500).body(Map.of("message", "시스템 오류가 발생했습니다."));
        }
    }
}