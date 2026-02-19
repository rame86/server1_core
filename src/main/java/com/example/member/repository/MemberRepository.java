package com.example.member.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
	// 널값 방지 Optional
    Optional<Member> findByKakaoId(String kakaoId);
    Optional<Member> findByNaverId(String naverId);
    Optional<Member> findByGoogleId(String googleId);
}
