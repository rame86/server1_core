// package com.example.artist.service;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Optional;
// import java.util.UUID;
// import java.util.stream.Collectors;

// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import com.example.admin.dto.approval.ArtistResultDTO;
// import com.example.artist.dto.ArtistResponse;
// import com.example.artist.dto.PaymentRequestDTO;
// import com.example.artist.entity.Artist;
// import com.example.artist.entity.Donation;
// import com.example.artist.entity.Follow;
// import com.example.artist.entity.constant.DonationStatus;
// import com.example.artist.messaging.producer.ArtistEventProducer;
// import com.example.artist.repository.ArtistRepository;
// import com.example.artist.repository.DonationRepository;
// import com.example.artist.repository.FollowRepository;
// import com.example.config.RabbitMQConfig;
// import com.example.member.domain.Member;
// import com.example.member.repository.MemberRepository;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class ArtistService {

// 	private final ArtistRepository artistRepository;
// 	private final FollowRepository followRepository;
// 	private final MemberRepository memberRepository;
// 	private final ArtistEventProducer artistEventProducer;
// 	private final DonationRepository donationRepository;
	
// 	@Transactional
// 	public List<ArtistResponse> getAllArtist(){
// 		List<Artist> artists = artistRepository.findAllByOrderByMemberCreatedAtDesc();
// 		return artists.stream().map(artist -> {
//             ArtistResponse dto = new ArtistResponse();
//             dto.setMemberId(artist.getMemberId());
//             dto.setStageName(artist.getStageName());
//             dto.setDescription(artist.getDescription());
//             dto.setCommunityLink(artist.getCommunityLink());
//             dto.setFollowerCount(followRepository.countByArtist(artist));
//             dto.setCategory(artist.getCategory());
//             dto.setProfileImageUrl(artist.getProfileImageUrl());
//             dto.setFandomImage(artist.getFandomImage());
//             dto.setFandomName(artist.getFandomName());
//             return dto;
//         }).collect(Collectors.toList());
// 	}

// 	/**
//      * [추가] 아티스트 상세 조회 메서드
//      */
//     @Transactional(readOnly = true)
//     public ArtistResponse getArtistById(Long artistId) {
//         log.info("-----> [Service] 아티스트 상세 조회 시작: {}", artistId);
//         Artist artist = artistRepository.findByMemberId(artistId);
        
//         if (artist == null) {
//             log.error("해당 ID의 아티스트를 찾을 수 없습니다: {}", artistId);
//             throw new RuntimeException("Artist not found"); 
//         }
//         return convertToResponse(artist);
//     }

// 	@Transactional(readOnly = true)
// 	public List<ArtistResponse> getFollowedArtists(Long memberId) {
// 		Member member = memberRepository.findByMemberId(memberId);
// 		return followRepository.findAllByMember(member).stream().map(follow -> {
// 			Artist artist = follow.getArtist();
// 			ArtistResponse dto = new ArtistResponse();
// 			dto.setMemberId(artist.getMemberId());
// 			dto.setStageName(artist.getStageName());
// 			dto.setDescription(artist.getDescription());
// 			dto.setCommunityLink(artist.getCommunityLink());
// 			dto.setProfileImageUrl(artist.getProfileImageUrl());
// 			dto.setFollowerCount(followRepository.countByArtist(artist));
// 			return dto;
// 		}).collect(Collectors.toList());
// 	}
	
// 	@Transactional
// 	public String toggleFollow(Long memberId, Long artistId) {
// 		Member follower = memberRepository.findByMemberId(memberId);
// 		log.info(follower.getMemberId().toString());
// 		Artist artist = artistRepository.findByMemberId(artistId);
		
// 		Optional<Follow> existingFollow = followRepository.findByMemberAndArtist(follower, artist);
		
// 		if(existingFollow.isPresent()) {
// 			followRepository.delete(existingFollow.get());
// 			artist.setFollowerCount(Math.max(0, artist.getFollowerCount() - 1));
// 			log.info("언팔로우 완료 : {} -> {}", follower.getName(), artist.getStageName());
// 			return "UNFOLLOW_SUCCESS";
// 		} else {
// 			Follow follow = new Follow();
// 			follow.setMember(follower);
// 			follow.setArtist(artist);
// 			follow.setCreatedAt(LocalDateTime.now());
// 			followRepository.save(follow);
// 			artist.setFollowerCount(artist.getFollowerCount() + 1);
// 			log.info("팔로우 완료: {} -> {}", follower.getName(), artist.getStageName());
// 			return "FOLLOW_SUCCESS";
// 		}
// 	}
	
// 	@Transactional
// 	public String donateToArtist(Long memberId, Long artistId, BigDecimal amount) {
		
// 		// 주문번호 생성
// 		String orderId = "DONO-" + UUID.randomUUID().toString().substring(0, 8);
		
// 		// entity에 먼저 저장
// 		Donation donation = Donation.builder()
// 				.orderId(orderId)
// 				.memberId(memberId)
// 				.artistId(artistId)
// 				.amount(amount)
// 				.status(DonationStatus.READY)
// 				.build();
// 		donationRepository.save(donation);
		
// 		// 아티스트 이름 조회
// 		Artist artist = artistRepository.findByMemberId(artistId);
// 		String artistDisplayName = (artist != null && artist.getStageName() != null)
// 				? artist.getStageName()
// 				: artistId + "번 아티스트";

// 		// DTO 조립, MQ로 전송
// 		PaymentRequestDTO requestDTO = PaymentRequestDTO.builder()
// 				.orderId(orderId)
// 				.memberId(memberId)
// 				.artistId(artistId)
// 				.amount(amount)
// 				.type("DONATION")
// 				.eventTitle(artistDisplayName + " 아티스트 후원")
// 				.replyRoutingKey(RabbitMQConfig.PAY_RES_ROUTING_KEY)
// 				.build();
// 		artistEventProducer.sendPaymentRequest(requestDTO);
		
// 		// status를 진행중으로 바꿔주기
// 		donation.processing();
		
// 		log.info("-----> [도네이션 요청 완료] 주문번호: {}", orderId);
// 		return orderId;
// 	}

// 	/**
//      * [추가] 엔티티를 DTO로 변환하는 공통 메서드 (중복 코드 제거)
//      */
//     private ArtistResponse convertToResponse(Artist artist) {
//         ArtistResponse dto = new ArtistResponse();
//         dto.setMemberId(artist.getMemberId());
//         dto.setStageName(artist.getStageName());
//         dto.setDescription(artist.getDescription());
//         dto.setCommunityLink(artist.getCommunityLink());
//         dto.setProfileImageUrl(artist.getProfileImageUrl());
//         dto.setFollowerCount(followRepository.countByArtist(artist));
//         dto.setCategory(artist.getCategory());
//         dto.setFandomImage(artist.getFandomImage());
//         dto.setFandomName(artist.getFandomName());
//         return dto;
//     }

// 	//--------------------------------------------------------------------------------------------------------------------------
//     //--------------------------------------------------------------------------------------------------------------------------
// 	// 수민 수정내용
// 	// ArtistProfile 팬덤 이미지 수정
//     // 팬덤 브랜딩(이름, 이미지) 전용 수정 API
// 	// 트랜잭션 안에서 엔티티를 수정하면 더티 체킹으로 자동 UPDATE 쿼리 실행됨
//     @Transactional
//     public void updateFandomInfo(Long memberId, ArtistResultDTO dto) {
//         // 아티스트 검증 및 조회 (엔티티가 Member면 MemberRepository 사용)
//         Artist artist = artistRepository.findByMemberId(memberId);

//         // 핵심 주석: 객체가 null인지 직접 확인해서 예외 처리
//         if (artist == null) {
//             throw new IllegalArgumentException("아티스트 정보를 찾을 수 없어.");
//         }

//         // 2. 입력된 값만 안전하게 업데이트 (Null 및 빈 문자열 방어)
//         if (dto.getFandomName() != null && !dto.getFandomName().trim().isEmpty()) {
//             artist.setFandomName(dto.getFandomName());
//         }
        
//         // 주의: DTO 필드명은 fandomImage, 엔티티 필드명에 맞게 세팅
//         if(dto.getFandomImage() != null && !dto.getFandomImage().trim().isEmpty()) {
//             artist.setFandomImage(dto.getFandomImage()); 
//         }
//     }
// 	//--------------------------------------------------------------------------------------------------------------------------
//     //--------------------------------------------------------------------------------------------------------------------------


	
	
// }
