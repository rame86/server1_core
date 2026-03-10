package com.example.admin.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.entity.Approval;
import com.example.admin.repository.ApprovalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

	private final ApprovalRepository approvalRepository;
	
	@Transactional
	public void updateApprovalStatus(Long approvalId, String status) {
		Approval approval = approvalRepository.findById(approvalId)
				.orElseThrow(() -> new RuntimeException("해당 신청 건을 찾을 수 없습니다."));
		
		approval.setStatus(status);
		approval.setProcessedAt(LocalDateTime.now());
	}
}
