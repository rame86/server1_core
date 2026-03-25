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

import com.example.admin.dto.ArtistResultDTO;
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
    
    //--------------------------------------------
    private final FileUploadService fileUploadService;  //수민 추가내용
    //--------------------------------------------

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
        return ResponseEntity.ok(artistService.toggleFollow(user.getMemberId(), artistId));
    }
    
    // 아티스트 상세정보 조회
    @GetMapping("/{id}")
    public ResponseEntity<ArtistResponse> getArtistDetail(@PathVariable("id") Long id) {
        log.info("-----> [ARTIST DETAIL REQUEST] 아티스트ID: {}", id);
        ArtistResponse artist = artistService.getArtistById(id); // 서비스에 메서드 구현 필요
        return ResponseEntity.ok(artist);
    }
     
    /**
     * [추가] 아티스트별 팬레터(게시글) 목록 조회
     * GET /artist/{id}/fan-letters
     */
    @GetMapping("/{id}/fan-letters")
    public ResponseEntity<?> getArtistFanLetters(
            @LoginUser RedisMemberDTO user,
            @PathVariable("id") Long id) {
        
        Long memberId = (user != null) ? user.getMemberId() : null;
        log.info("-----> [FAN-LETTERS REQUEST] 아티스트ID: {}", id);
        
        // 기존 boardService를 활용하거나 아티스트별 필터링 로직 호출
        List<BoardDTO> list = artistBoardService.getBoardListByArtist(id, memberId); 
        return ResponseEntity.ok(list);
    }

    /**
     * [추가] 아티스트별 공지사항 조회
     * GET /artist/{id}/notices
     */
    @GetMapping("/{id}/notices")
    public ResponseEntity<?> getArtistNotices(@PathVariable("id") Long id) {
        log.info("-----> [NOTICES REQUEST] 아티스트ID: {}", id);
        List<BoardDTO> notices = artistBoardService.getNoticeListByArtist(id); // 서비스 구현 필요
        return ResponseEntity.ok(notices);
    }

     /**
     * 아티스트별 게시판 목록 조회
     * BoardService의 getBoardList(String category) 정의에 맞춰 호출부를 수정했습니다.
     
    @GetMapping("/board/{artistId}")
    public ResponseEntity<?> getArtistBoard(
            @LoginUser RedisMemberDTO user,
            @RequestParam(name = "category", required = false, defaultValue = "전체") String category,
            @PathVariable(name = "artistId") Long artistId) {
        
        // [수정] 로그인 안 한 유저면 null을 보내도록 처리
        Long memberId = (user != null) ? user.getMemberId() : null;
        log.info("-----> [BOARD LIST REQUEST] 카테고리: {}, 아티스트ID: {}, 요청자: {}", category, artistId, user.getMemberId());
        
        // BoardService에서 파라미터를 하나(category)만 받도록 수정되었으므로 이에 맞춰 호출합니다.
        // 만약 아티스트별 필터링이 반드시 필요하다면 BoardService에 artistId를 받는 로직을 추가해야 합니다.
        List<BoardDTO> list = boardService.getBoardList(category, memberId);
        return ResponseEntity.ok(list);
    }*/
    
    // 후원 테스트
    @PostMapping("/donate")
    public ResponseEntity<String> donate(@LoginUser RedisMemberDTO user, @RequestBody DonationRequset req) {
    	log.info("현재 로그인한 유저의 잔액: {}", user.getBalance());
    	if(BigDecimal.valueOf(user.getBalance()).compareTo(req.getAmount()) < 0) {
    		return ResponseEntity.badRequest().body("잔액이 부족합니다.");
    	}
    	log.info("artistService 호출");
    	String orderId = artistService.donateToArtist(user.getMemberId(), req.getArtistId(), req.getAmount());
    	return ResponseEntity.ok("후원 요청 완료! 주문번호: " + orderId);
    }
    
	// 아티스트 신청 (별도 버튼 클릭 시)
	@PostMapping("/apply")
	public ResponseEntity<?> artistApply(@LoginUser RedisMemberDTO user, @RequestBody ArtistResultDTO dto) {
		if(user == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요한 서비스입니다."));
		try {
			memberService.applyArtist(user.getMemberId(), dto);
			log.info("-----> [아티스트 신청 접수] 요청자 ID: {}", user.getMemberId());
			return ResponseEntity.ok(Map.of("message", "아티스트 신청이 성공적으로 접수되었습니다. 심사 후 알림을 드릴게요!"));			
		} catch (IllegalStateException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			log.error("아티스트 신청 중 서버 오류 발생: ", e);
            return ResponseEntity.status(500).body(Map.of("message", "시스템 오류가 발생했습니다. 다시 시도해주세요."));
		}
	}

    //--------------------------------------------------------------------------------------------------------------------------
    // 수민 수정내용
    // 아티스트 배경(팬덤) 이미지 사전 업로드 API
    @PostMapping("/bg-image") // 경로는 네 맘대로 맞춰! (예: /artist/bg-image)
    public ResponseEntity<?> uploadArtistBgImage(@LoginUser RedisMemberDTO user, @RequestParam("bgImageFile") MultipartFile file) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            // "artist" 폴더에 저장하고 URL 반환
            String url = fileUploadService.uploadImage(file, "artist");
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("아티스트 이미지 업로드 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "이미지 업로드에 실패했습니다."));
        }
    }
    //--------------------------------------------------------------------------------------------------------------------------
    
}