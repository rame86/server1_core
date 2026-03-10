package com.example.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.admin.entity.Approval;

public interface ApprovalRepository extends JpaRepository<Approval, Long>{

}
