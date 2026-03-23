package com.example.artist.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.artist.dto.ArtistResponse;
import com.example.artist.dto.PaymentRequestDTO;
import com.example.artist.entity.Artist;
import com.example.artist.entity.Donation;
import com.example.artist.entity.Follow;
import com.example.artist.entity.constant.DonationStatus;
import com.example.artist.messaging.producer.ArtistEventProducer;
import com.example.artist.repository.ArtistRepository;
import com.example.artist.repository.DonationRepository;
import com.example.artist.repository.FollowRepository;
import com.example.config.RabbitMQConfig;
import com.example.member.domain.Member;
import com.example.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistService {

	private final ArtistRepository artistRepository;
	private final FollowRepository followRepository;
	private final MemberRepository memberRepository;
	private final ArtistEventProducer artistEventProducer;
	private final DonationRepository donationRepository;
	
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

	@Transactional(readOnly = true)
	public List<ArtistResponse> getFollowedArtists(Long memberId) {
		Member member = memberRepository.findByMemberId(memberId);
		return followRepository.findAllByMember(member).stream().map(follow -> {
			Artist artist = follow.getArtist();
			ArtistResponse dto = new ArtistResponse();
			dto.setMemberId(artist.getMemberId());
			dto.setStageName(artist.getStageName());
			dto.setDescription(artist.getDescription());
			dto.setCommunityLink(artist.getCommunityLink());
			dto.setProfileImageUrl(artist.getProfileImageUrl());
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
			artist.setFollowerCount(Math.max(0, artist.getFollowerCount() - 1));
			log.info("언팔로우 완료 : {} -> {}", follower.getName(), artist.getStageName());
			return "UNFOLLOW_SUCCESS";
		} else {
			Follow follow = new Follow();
			follow.setMember(follower);
			follow.setArtist(artist);
			follow.setCreatedAt(LocalDateTime.now());
			followRepository.save(follow);
			artist.setFollowerCount(artist.getFollowerCount() + 1);
			log.info("팔로우 완료: {} -> {}", follower.getName(), artist.getStageName());
			return "FOLLOW_SUCCESS";
		}
	}
	
	@Transactional
	public String donateToArtist(Long memberId, Long artistId, BigDecimal amount) {
		
		// 주문번호 생성
		String orderId = "DONO-" + UUID.randomUUID().toString().substring(0, 8);
		
		// entity에 먼저 저장
		Donation donation = Donation.builder()
				.orderId(orderId)
				.memberId(memberId)
				.artistId(artistId)
				.amount(amount)
				.status(DonationStatus.READY)
				.build();
		donationRepository.save(donation);
		
		// DTO 조립, MQ로 전송
		PaymentRequestDTO requestDTO = PaymentRequestDTO.builder()
				.orderId(orderId)
				.memberId(memberId)
				.artistId(artistId)
				.amount(amount)
				.type("DONATION")
				.eventTitle(artistId + "번 아티스트 후원")				
				.replyRoutingKey(RabbitMQConfig.PAY_RES_ROUTING_KEY)
				.build();
		artistEventProducer.sendPaymentRequest(requestDTO);
		
		// status를 진행중으로 바꿔주기
		donation.processing();
		
		log.info("-----> [도네이션 요청 완료] 주문번호: {}", orderId);
		return orderId;
	}
	
}
