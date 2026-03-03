package com.example.artist.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.artist.entity.Artist;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
	List<Artist> findAllByOrderByMemberCreatedAtDesc();
}
