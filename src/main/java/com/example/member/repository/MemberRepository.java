package com.example.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
	// 널값 방지 Optional
    Optional<Member> findByEmail(String email);
    
    // memberId값으로 찾기
    Member findByMemberId(Long memberId);
}
