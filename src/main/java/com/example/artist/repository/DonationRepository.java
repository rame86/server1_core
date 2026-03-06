package com.example.artist.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.artist.entity.Donation;

public interface DonationRepository extends JpaRepository<Donation, Long>{
	Optional<Donation> findByOrderId(String orderId);
}
