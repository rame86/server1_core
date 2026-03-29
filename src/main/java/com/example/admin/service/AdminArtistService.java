package com.example.admin.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.admin.dto.approval.ApprovalDTO;
import com.example.admin.dto.approval.ArtistApprovedEvent;
import com.example.admin.dto.approval.ArtistResultDTO;
import com.example.admin.dto.artist.ArtistAccountResponse;
import com.example.admin.dto.artist.ArtistResponseDTO;
import com.example.admin.entity.Approval;
import com.example.admin.repository.ApprovalRepository;
import com.example.artist.entity.Artist;
import com.example.artist.repository.ArtistRepository;
import com.example.config.RabbitMQConfig;
import com.example.member.domain.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminArtistService {
	
	private final RabbitTemplate rabbitTemplate;
	private final WebClient webClient;
	private final ApprovalRepository approvalRepository;
	private final ArtistRepository artistRepository;
	private final AdminApprovalService adminApprovalService;
	private final AdminUserService adminUserService;
	private final AdminNotifyService adminNotifyService;
	
	@Value("${pay.admin.url}")
	private String payAdminUrl;

	// artist 승인
	@Transactional
	public void confirmArtist(ArtistResultDTO dto, Long adminId) {
		// 신청서 조회 및 검증
		Approval approval = adminApprovalService.findApproval(dto.getApprovalId());

		if (!"PENDING".equals(approval.getStatus())) {
			throw new IllegalStateException("이미 처리되었거나 취소된 신청건입니다.");
		}

		if (artistRepository.existsById(approval.getArtistId())) {
			throw new IllegalStateException("이미 아티스트로 등록된 회원입니다.");
		}
		
		// 상태 업데이트
		dto.setStatus("CONFIRMED");
		adminApprovalService.updateApprovalStatus(dto, adminId);

		// MEMBER테이블 권한 변경
		Member member = adminUserService.findMember(approval.getArtistId());
		member.setRole("ARTIST");
		member.setStatus("ACTIVE");

		// ARTIST테이블 INSERT
		Artist artist = new Artist();
		artist.setMember(member);
		artist.setStageName(approval.getRequesterName());
		artist.setCategory(approval.getSubCategory());
		artist.setDescription(approval.getDescription());
		artist.setFandomImage(approval.getFandomImage());
		artist.setFandomName(approval.getFandomName());
		artistRepository.save(artist);

		// 2서버로 메세지 발송
		ArtistApprovedEvent event = ArtistApprovedEvent.builder()
				.memberId(member.getMemberId())
				.artistName(approval.getRequesterName())
				.type("ARTIST_APPROVE")
				.build();

		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PAY_REQ_ROUTING_KEY, event);

		log.info("-----> [ARTIST 완료] 1서버 DB 갱신, 메세지 전송 완료");
		
		String adminMsg = approval.getRequesterName() + " 님이 아티스트로 최종 승인되었습니다!";
		adminNotifyService.sendAlert(adminMsg);
		
		String userMsg = "축하합니다! 아티스트 승인 신청이 수락되었습니다.";
		adminNotifyService.sendUserAlert(approval.getArtistId(), userMsg);
		
	}

	// artist 승인 대기중인 목록
	public List<ArtistResultDTO> getPendingArtistList(String category, String status) {
		List<Approval> entityList = approvalRepository.findByCategoryAndStatus(category, status);
		return entityList.stream()
				.map(entity -> ArtistResultDTO.builder().approvalId(entity.getApprovalId())
						.artistName(entity.getRequesterName()).subCategory(entity.getSubCategory())
						.description(entity.getDescription()).imageUrl(entity.getImageUrl()).status(entity.getStatus())
						.createdAt(entity.getCreatedAt().toString())
						// 핵심 주석: processedAt이 null일 때 toString() 호출을 막는 방어 로직
						.processedAt(entity.getProcessedAt() != null ? entity.getProcessedAt().toString() : null)
						.rejectionReason(entity.getRejectionReason()).build())
				.collect(Collectors.toList());
	}

	// artist 상세 보기
	public ArtistResponseDTO getArtistDetail(Long approvalId, Long artistId) {

		Approval approval = adminApprovalService.findApproval(approvalId);
		Artist artist = artistRepository.findById(artistId)
	            .orElseThrow(() -> new IllegalArgumentException("아티스트를 찾을 수 없습니다."));
		Member member = adminUserService.findMember(artist.getMemberId());
		
		// 2서버 WebClient 호출
		ArtistAccountResponse paymentData = webClient.get()
				.uri(payAdminUrl + "/artist/{artistId}", artistId)
				.retrieve()
				.bodyToMono(ArtistAccountResponse.class)
				.onErrorResume(e -> Mono.just(new ArtistAccountResponse(BigDecimal.ZERO, BigDecimal.ZERO)))
				.block();

		return ArtistResponseDTO.builder()
				.approvalId(approval.getApprovalId())
				.artistId(member.getMemberId())
				.artistName(member.getName())
				.category(approval.getCategory())
				.description(approval.getDescription())
				.createdAt(approval.getCreatedAt()
				.toString())
				.status(approval.getStatus())
				.email(member.getEmail())
				.phone(member.getPhone())
				.followerCount(artist.getFollowerCount())
				.totalBalance(paymentData.getTotalBalance())
				.withdrawableBalance(paymentData.getWithdrawableBalance())
				.rejectionReason(approval.getRejectionReason())
				.adminId(approval.getAdminId())
				.processedAt(approval.getProcessedAt() != null ? approval.getProcessedAt().toString() : "처리 대기중")
				.build();
	}

	// artist 승인된 리스트
	@Transactional(readOnly = true)
	public List<ArtistResponseDTO> getActiceArtistList(String category, String status) {
		List<Approval> entityList = approvalRepository.findByCategoryAndStatus(category, status);

		return entityList.stream().map(entity -> ArtistResponseDTO.builder()
				.approvalId(entity.getApprovalId())
				.artistId(entity.getArtistId())
				.artistName(entity.getRequesterName())
				.category(entity.getSubCategory())
				.status(entity.getStatus())
				.createdAt(entity.getCreatedAt().toString())
				.processedAt(entity.getProcessedAt() != null ? entity.getProcessedAt().toString() : null)
				.adminId(entity.getAdminId())
				.build())
				.collect(Collectors.toList());
	}
	
	public Artist findArtist(Long artistId) {
		return artistRepository.findByArtistId(artistId)
				.orElseThrow(() -> new IllegalArgumentException("아티스트를 찾을 수 없습니다. ID: " + artistId));
	}
	
	// artist 거절
	@Transactional
	public void rejectArtist(ArtistResultDTO dto, Long adminId) {
		dto.setStatus("REJECTED");
		adminApprovalService.updateApprovalStatus((ApprovalDTO)dto, adminId);
		
		Approval approval = adminApprovalService.findApproval(dto.getApprovalId());
		Member member = adminUserService.findMember(approval.getArtistId());
		
		if ("PENDING".equals(member.getStatus())) {
			member.setStatus("ACTIVE");
			log.info("-----> [상태 복구] MemberID: {} 님이 일반 유저(ACTIVE)로 전환되었습니다.", member.getMemberId());
		}

		log.info("-----> [거절 완료] ID: {}, 사유: {}", dto.getApprovalId(), dto.getRejectionReason());
	}

}
