package com.example.admin.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.admin.dto.event.AdminEventListDTO;
import com.example.admin.entity.Approval;
import com.example.admin.repository.ApprovalRepository;
import com.example.artist.repository.ArtistRepository;
import com.example.member.repository.MemberHistoryRepository;
import com.example.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEventService {
	
	private final ApprovalRepository approvalRepository;
    private final StringRedisTemplate redisTemplate;
	
	@Transactional(readOnly = true)
	public List<AdminEventListDTO> getAllEvents() {
		List<Approval> approvals = approvalRepository.findAllByOrderByCreatedAtDesc();

		return approvals.stream().map(approval -> {
			// redis에서 실시간 잔여석 조회하기
			String redisKey = "event:stock:" + approval.getTargetId();
			String stockVal = redisTemplate.opsForValue().get(redisKey);
			int currentStock = (stockVal != null) ? Integer.parseInt(stockVal) : 0;

			// DTO로 변환
			return AdminEventListDTO.builder().approvalId(approval.getApprovalId())
					.artistname(approval.getRequesterName()).targetId(approval.getTargetId()).title(approval.getTitle())
					.status(approval.getStatus()).category(approval.getCategory()).location(approval.getLocation())
					.price(approval.getPrice()).eventStartDate(approval.getEventStartDate())
					.eventEndDate(approval.getEventEndDate())
					.eventDate(approval.getEventDate())
					.totalCapacity(approval.getStock())
					.createdAt(approval.getCreatedAt()).stock(currentStock) // Redis 데이터 합체
					.imageUrl(approval.getImageUrl()).build();
		}).collect(Collectors.toList());
	}

}
