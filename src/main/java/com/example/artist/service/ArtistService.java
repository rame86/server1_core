package com.example.artist.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.dto.approval.ArtistResultDTO;
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
    
    /**
     * 전체 아티스트 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ArtistResponse> getAllArtist(){
        List<Artist> artists = artistRepository.findAllByOrderByMemberCreatedAtDesc();
        return artists.stream()
                .map(this::convertToResponse) // 공통 변환 메서드 활용
                .collect(Collectors.toList());
    }

    /**
     * 아티스트 상세 조회
     */
    @Transactional(readOnly = true)
    public ArtistResponse getArtistById(Long artistId) {
        log.info("-----> [Service] 아티스트 상세 조회 시작: {}", artistId);
        Artist artist = artistRepository.findByMemberId(artistId);
        
        if (artist == null) {
            log.error("해당 ID의 아티스트를 찾을 수 없습니다: {}", artistId);
            throw new RuntimeException("Artist not found"); 
        }
        return convertToResponse(artist);
    }

    /**
     * 내가 팔로우한 아티스트 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ArtistResponse> getFollowedArtists(Long memberId) {
        Member member = memberRepository.findByMemberId(memberId);
        return followRepository.findAllByMember(member).stream()
                .map(follow -> convertToResponse(follow.getArtist())) // 공통 변환 메서드 활용
                .collect(Collectors.toList());
    }
    
    /**
     * 팔로우 / 언팔로우 토글
     */
    @Transactional
    public String toggleFollow(Long memberId, Long artistId) {
        Member follower = memberRepository.findByMemberId(memberId);
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
    
    /**
     * 아티스트에게 후원하기 (MQ 연동)
     */
    @Transactional
    public String donateToArtist(Long memberId, Long artistId, BigDecimal amount) {
        String orderId = "DONO-" + UUID.randomUUID().toString().substring(0, 8);
        
        Donation donation = Donation.builder()
                .orderId(orderId)
                .memberId(memberId)
                .artistId(artistId)
                .amount(amount)
                .status(DonationStatus.READY)
                .build();
        donationRepository.save(donation);
        
        Artist artist = artistRepository.findByMemberId(artistId);
        String artistDisplayName = (artist != null && artist.getStageName() != null)
                ? artist.getStageName()
                : artistId + "번 아티스트";

        PaymentRequestDTO requestDTO = PaymentRequestDTO.builder()
                .orderId(orderId)
                .memberId(memberId)
                .artistId(artistId)
                .amount(amount)
                .type("DONATION")
                .eventTitle(artistDisplayName + " 아티스트 후원")
                .replyRoutingKey(RabbitMQConfig.PAY_RES_ROUTING_KEY)
                .build();
        artistEventProducer.sendPaymentRequest(requestDTO);
        
        donation.processing();
        log.info("-----> [도네이션 요청 완료] 주문번호: {}", orderId);
        return orderId;
    }

    /**
     * [공통] 엔티티를 DTO로 변환하는 메서드
     */
    private ArtistResponse convertToResponse(Artist artist) {
        ArtistResponse dto = new ArtistResponse();
        dto.setMemberId(artist.getMemberId());
        dto.setStageName(artist.getStageName());
        dto.setDescription(artist.getDescription());
        dto.setCommunityLink(artist.getCommunityLink());
        dto.setProfileImageUrl(artist.getProfileImageUrl());
        dto.setFollowerCount(followRepository.countByArtist(artist)); // 실제 팔로우 수 반영
        dto.setCategory(artist.getCategory());
        dto.setFandomImage(artist.getFandomImage());
        dto.setFandomName(artist.getFandomName());
        return dto;
    }

    //--------------------------------------------------------------------------------------------------------------------------
    // 수민 님 수정내용 및 팬덤 브랜딩 관리
    //--------------------------------------------------------------------------------------------------------------------------

    /**
     * 팬덤 브랜딩(이름, 이미지) 수정
     * @Transactional 어노테이션을 통해 변경 감지(Dirty Checking)로 자동 업데이트
     */
    @Transactional
    public void updateFandomInfo(Long memberId, ArtistResultDTO dto) {
        Artist artist = artistRepository.findByMemberId(memberId);

        if (artist == null) {
            throw new IllegalArgumentException("아티스트 정보를 찾을 수 없어.");
        }

        // 1. 팬덤 이름 업데이트
        if (dto.getFandomName() != null && !dto.getFandomName().trim().isEmpty()) {
            artist.setFandomName(dto.getFandomName());
        }
        
        // 2. 팬덤 배경 이미지 업데이트
        if(dto.getFandomImage() != null && !dto.getFandomImage().trim().isEmpty()) {
            artist.setFandomImage(dto.getFandomImage()); 
        }
        
        log.info("-----> [Service] 팬덤 정보 수정 완료: 아티스트ID {}", memberId);
    }
    
    //--------------------------------------------------------------------------------------------------------------------------
}