package com.example.artist.repository;

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
}
