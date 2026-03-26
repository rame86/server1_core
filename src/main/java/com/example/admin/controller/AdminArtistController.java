package com.example.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.dto.approval.ArtistResultDTO;
import com.example.admin.dto.artist.ArtistResponseDTO;
import com.example.admin.service.AdminArtistService;
import com.example.common.annotation.LoginUser;
import com.example.member.dto.RedisMemberDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/admin/artist")
@RequiredArgsConstructor
public class AdminArtistController {
	
	private final AdminArtistService adminArtistService;

	@PostMapping("/confirm")
	public ResponseEntity<String> confirmArtist(@RequestBody ArtistResultDTO dto, @LoginUser RedisMemberDTO user) {
		log.info("=====> [1서버 관리자] 아티스트 등록 완료");
		adminArtistService.confirmArtist(dto, user.getMemberId());
		return ResponseEntity.ok("ARTIST 처리 완료");
	}

	@PostMapping("/reject")
	public ResponseEntity<String> rejectArtist(@RequestBody ArtistResultDTO dto, @LoginUser RedisMemberDTO user) {
		log.info("=====> [1서버 관리자] 아티스트 거절 처리");
		adminArtistService.rejectArtist(dto, user.getMemberId());
		return ResponseEntity.ok("ARTIST 거절 처리 완료");
	}

	// 승인요청한 아티스트 목록
	@GetMapping("/approvalList")
	public ResponseEntity<List<ArtistResultDTO>> getPendingArtist() {
		return ResponseEntity.ok(adminArtistService.getPendingArtistList("ARTIST", "PENDING"));
	}

	// 승인된 아티스트 목록
	@GetMapping("/activeList")
	public ResponseEntity<List<ArtistResponseDTO>> getActiveArtist() {
		return ResponseEntity.ok(adminArtistService.getActiceArtistList("ARTIST", "CONFIRMED"));
	}

	// 거절된 아티스트 목록
	@GetMapping("/rejectionList")
	public ResponseEntity<List<ArtistResultDTO>> getRejectedArtist() {
		return ResponseEntity.ok(adminArtistService.getPendingArtistList("ARTIST", "REJECTED"));
	}

	// 아티스트 상세 정보
	@GetMapping("/{approvalId}/{artistId}")
	public ResponseEntity<ArtistResponseDTO> getArtistDetail(
			@PathVariable("approvalId") Long approvalId,
			@PathVariable("artistId") Long artistId) {
		return ResponseEntity.ok(adminArtistService.getArtistDetail(approvalId, artistId));
	}

}
