package com.example.artist.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.artist.dto.ArtistResponse;
import com.example.artist.dto.DonationRequset;
import com.example.artist.service.ArtistService;
import com.example.board.service.BoardService;
import com.example.common.annotation.LoginUser;
import com.example.member.dto.RedisMemberDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/artist")
@RequiredArgsConstructor
public class ArtistController {
    
    private final ArtistService artistService;
    private final BoardService boardService;

    @GetMapping("/list")
    public ResponseEntity<List<ArtistResponse>> getList() {
        return ResponseEntity.ok(artistService.getAllArtist());
    }
    
    @PostMapping("/follow/{artistId}")
    public ResponseEntity<String> followArtist(
            @LoginUser RedisMemberDTO user,
            @PathVariable("artistId") Long artistId) {
        return ResponseEntity.ok(artistService.toggleFollow(user.getMemberId(), artistId));
    }
    
    /**
     * 아티스트별 게시판 목록 조회
     * BoardService의 getBoardList(String category) 정의에 맞춰 호출부를 수정했습니다.
     */
    @GetMapping("/board/{artistId}")
    public ResponseEntity<?> getArtistBoard(
            @LoginUser RedisMemberDTO user,
            @RequestParam(name = "category", required = false, defaultValue = "전체") String category,
            @PathVariable(name = "artistId") Long artistId) {
        
        log.info("-----> [BOARD LIST REQUEST] 카테고리: {}, 아티스트ID: {}, 요청자: {}", category, artistId, user.getMemberId());
        
        // BoardService에서 파라미터를 하나(category)만 받도록 수정되었으므로 이에 맞춰 호출합니다.
        // 만약 아티스트별 필터링이 반드시 필요하다면 BoardService에 artistId를 받는 로직을 추가해야 합니다.
        return ResponseEntity.ok(boardService.getBoardList(category));
    }
    
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
    
}