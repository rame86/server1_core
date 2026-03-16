package com.example.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.admin.entity.Approval;

public interface ApprovalRepository extends JpaRepository<Approval, Long>{
	List<Approval> findAllByOrderByCreatedAtDesc();
	List<Approval> findByCategoryAndStatus(String category, String status);
}