package com.example.artist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.artist.entity.Artist;
import com.example.artist.entity.Follow;
import com.example.member.domain.Member;

public interface FollowRepository extends JpaRepository<Follow, Long>{
	// 아티스트별 팔로워 수
	long countByArtist(Artist artistId);
	
	// 팔로우 중복 확인
	Optional<Follow> findByMemberAndArtist(Member memberId, Artist artistId);

	// 내가 팔로우한 아티스트 목록
	List<Follow> findAllByMember(Member member);

	// Follow 엔티티 내부의 필드명(followerId, artistId)과 일치해야 합니다.
	boolean existsByMember_MemberIdAndArtist_ArtistId(Long memberId, Long artistId);

}
