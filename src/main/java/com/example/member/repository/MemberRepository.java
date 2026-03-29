package com.example.member.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
	// 널값 방지 Optional
    Optional<Member> findByEmail(String email);
    // memberId값으로 찾기
    Member findByMemberId(Long memberId);
	long countByStatus(String status);
	
	@Query(value = 
		    "SELECT TO_CHAR(created_at, 'YYYY-MM') as month, COUNT(*) as users " +
		    "FROM member " +
		    "WHERE created_at >= CURRENT_DATE - INTERVAL '6 months' " +
		    "GROUP BY TO_CHAR(created_at, 'YYYY-MM') " +
		    "ORDER BY month ASC", 
		    nativeQuery = true)
	List<Map<String, Object>> getMonthlyUserGrowth();
	
	Page<Member> findByStatus(String status, Pageable pageable);
}
