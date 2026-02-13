package com.example.test.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.test.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
	// 널값 방지 Optional
    Optional<Member> findByKakaoId(Long kakaoId);
}
