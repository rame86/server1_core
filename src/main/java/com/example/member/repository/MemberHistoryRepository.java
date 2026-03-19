package com.example.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.member.domain.MemberHistory;

public interface MemberHistoryRepository extends JpaRepository<MemberHistory, Long>{

}
