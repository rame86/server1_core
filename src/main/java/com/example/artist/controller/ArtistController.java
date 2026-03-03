package com.example.artist.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.artist.dto.ArtistResponse;
import com.example.artist.service.ArtistService;
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
	
}
