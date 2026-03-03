package com.example.artist.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.artist.dto.ArtistResponse;
import com.example.artist.entity.Artist;
import com.example.artist.entity.Follow;
import com.example.artist.repository.ArtistRepository;
import com.example.artist.repository.FollowRepository;
import com.example.member.entity.Member;
import com.example.member.repository.MemberRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistService {

	private final ArtistRepository artistRepository;
	private final FollowRepository followRepository;
	private final MemberRepository memberRepository;
	
	@Transactional
	public List<ArtistResponse> getAllArtist(){
		List<Artist> artists = artistRepository.findAllByOrderByMemberCreatedAtDesc();
		return artists.stream().map(artist -> {
            ArtistResponse dto = new ArtistResponse();
            dto.setMemberId(artist.getMemberId());
            dto.setStageName(artist.getStageName());
            dto.setDescription(artist.getDescription());
            dto.setCommunityLink(artist.getCommunityLink());
            dto.setFollowerCount(followRepository.countByArtist(artist));
            return dto;
        }).collect(Collectors.toList());
	}
	
	@Transactional
	public String toggleFollow(Long memberId, Long artistId) {
		Member follower = memberRepository.findByMemberId(memberId);
		log.info(follower.getMemberId().toString());
		Artist artist = artistRepository.findByMemberId(artistId);
		
		Optional<Follow> existingFollow = followRepository.findByMemberAndArtist(follower, artist);
		
		if(existingFollow.isPresent()) {
			followRepository.delete(existingFollow.get());
			log.info("언팔로우 완료 : {} -> {}", follower.getName(), artist.getStageName());
			return "UNFOLLOW_SUCCESS";
		} else {
			Follow follow = new Follow();
			follow.setMember(follower);
			follow.setArtist(artist);
			follow.setCreatedAt(LocalDateTime.now());
			followRepository.save(follow);
			log.info("팔로우 완료: {} -> {}", follower.getName(), artist.getStageName());
			return "FOLLOW_SUCCESS";
		}
	}
}
