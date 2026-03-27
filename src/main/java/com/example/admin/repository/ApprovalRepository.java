package com.example.admin.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.admin.entity.Approval;

public interface ApprovalRepository extends JpaRepository<Approval, Long>{
	List<Approval> findAllByOrderByCreatedAtDesc();
	List<Approval> findByCategoryAndStatus(String category, String status);
	Optional<Approval> findByArtistIdAndStatus(Long artistId, String status);
	Optional<Approval> findFirstByArtistIdAndCategoryAndStatusOrderByProcessedAtDesc(Long artistId, String category, String status);
	boolean existsByArtistIdAndCategoryAndStatus(Long artistId, String category, String status);
	long countByCategoryAndStatus(String category, String status);
}