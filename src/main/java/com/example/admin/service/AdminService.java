package com.example.admin.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.admin.dto.AdminEventListDTO;
import com.example.admin.dto.ApprovalDTO;
import com.example.admin.dto.ArtistAccountResponse;
import com.example.admin.dto.ArtistApprovedEvent;
import com.example.admin.dto.ArtistResponseDTO;
import com.example.admin.dto.ArtistResultDTO;
import com.example.admin.dto.EventResultDTO;
import com.example.admin.dto.SettlementDashboardResponse;
import com.example.admin.dto.ShopResultDTO;
import com.example.admin.entity.Approval;
import com.example.admin.repository.ApprovalRepository;
import com.example.artist.dto.PaymentRequestDTO;
import com.example.artist.entity.Artist;
import com.example.artist.repository.ArtistRepository;
import com.example.config.RabbitMQConfig;
import com.example.member.domain.Member;
import com.example.member.repository.MemberHistoryRepository;
import com.example.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

	private final ApprovalRepository approvalRepository;
	private final RabbitTemplate rabbitTemplate;
	private final StringRedisTemplate redisTemplate;
	private final Map<String, CompletableFuture<SettlementDashboardResponse>> pendingRequests = new ConcurrentHashMap<>();
	private static final int MQ_TIMEOUT_SECONDS = 1;
	private final MemberRepository memberRepository;
	private final WebClient webClient;
	private final ArtistRepository artistRepository;
	private final MemberHistoryRepository memberHistoryRepository;
	private final PasswordEncoder passwordEncoder;
	private final com.fasterxml.jackson.databind.ObjectMapper objectMapper; // JSON 파싱용

	@Value("${pay.admin.url}")
	private String payAdminUrl;

	@Transactional
	public void processApproval(ApprovalDTO dto, String routingKey, Long adminId) {
		Long eventId = updateApprovalStatus(dto, adminId);
		if (dto instanceof EventResultDTO eventResultDto) {
			eventResultDto.setApprovalId(eventId);
		}
		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, dto);
	}

	private Long updateApprovalStatus(ApprovalDTO dto, Long adminId) {
		Approval approval = approvalRepository.findById(dto.getApprovalId())
				.orElseThrow(() -> new RuntimeException("해당 신청 건을 찾을 수 없습니다."));

		approval.setStatus(dto.getStatus());
		approval.setAdminId(adminId);
		approval.setProcessedAt(LocalDateTime.now());

		if ("REJECTED".equals(dto.getStatus())) {
			approval.setRejectionReason(dto.getRejectionReason());
		}

		return approval.getTargetId();
	}

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

	public SettlementDashboardResponse requestDashboardData() {
		String requestId = "ADMIN_SETTLEMENT_REQ";
		CompletableFuture<SettlementDashboardResponse> future = new CompletableFuture<>();
		pendingRequests.put(requestId, future);

		PaymentRequestDTO dto = PaymentRequestDTO.builder().type("ADMIN_SETTLEMENT")
				.replyRoutingKey(RabbitMQConfig.ADMIN_PAY_RES_ROUTING_KEY).build();
		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PAY_REQ_ROUTING_KEY, dto);

		try {
			return future.get(MQ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (Exception e) {
			pendingRequests.remove(requestId);
			return new SettlementDashboardResponse(null, null);
		}
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

	// 굿즈(Shop) 승인 대기 목록
	public List<ShopResultDTO> getPendingShopList(String category, String status) {
		List<Approval> entityList = approvalRepository.findByCategoryAndStatus(category, status);
		return entityList.stream().map(entity -> {
			try {
				// contentJson에 저장된 상세 정보를 ShopResultDTO로 복원
				ShopResultDTO dto = objectMapper.readValue(entity.getContentJson(), ShopResultDTO.class);

				// 엔티티의 기본 필드(ID, 상태, 날짜 등)로 덮어쓰기 (최신성 유지)
				return ShopResultDTO.builder().approvalId(entity.getApprovalId()).goodsId(entity.getTargetId())
						.requesterId(entity.getArtistId()).requesterName(entity.getRequesterName())
						.goodsName(entity.getTitle())
						.goodsType(entity.getSubCategory() != null ? entity.getSubCategory()
								: (dto != null ? dto.getGoodsType() : "UNKNOWN"))
						.description(entity.getDescription())
						.price(entity.getPrice() != null ? entity.getPrice().intValue()
								: (dto != null ? dto.getPrice() : 0))
						.color(dto != null ? dto.getColor() : null).size(dto != null ? dto.getSize() : null)
						.stockQuantity(dto != null ? dto.getStockQuantity() : 0).imageUrl(entity.getImageUrl())
						.status(entity.getStatus()).createdAt(entity.getCreatedAt().toString())
						.rejectionReason(entity.getRejectionReason()).build();
			} catch (Exception e) {
				log.error("Failed to parse shop approval JSON: {}", e.getMessage());
				// 파싱 실패 시 기본 정보라도 반환
				return ShopResultDTO.builder().approvalId(entity.getApprovalId()).goodsName(entity.getTitle())
						.status(entity.getStatus()).build();
			}
		}).collect(Collectors.toList());
	}

	// artist 승인
	@Transactional
	public void confirmArtist(ArtistResultDTO dto, Long adminId) {
		// 신청서 승인 상태로 변경
		Approval approval = approvalRepository.findById(dto.getApprovalId())
				.orElseThrow(() -> new IllegalArgumentException("신청서가 없습니다."));

		if (!"PENDING".equals(approval.getStatus())) {
			throw new IllegalStateException("이미 처리되었거나 취소된 신청건입니다.");
		}

		if (artistRepository.existsById(approval.getArtistId())) {
			throw new IllegalStateException("이미 아티스트로 등록된 회원입니다.");
		}

		approval.setStatus("CONFIRMED");
		approval.setAdminId(adminId);
		approval.setProcessedAt(LocalDateTime.now());

		// MEMBER테이블 권한 변경
		Member member = memberRepository.findById(approval.getArtistId())
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
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
		ArtistApprovedEvent event = ArtistApprovedEvent.builder().memberId(member.getMemberId())
				.artistName(approval.getRequesterName()).type("ARTIST_APPROVE").build();

		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PAY_REQ_ROUTING_KEY, event);

		log.info("-----> [ARTIST 완료] 1서버 DB 갱신, 메세지 전송 완료");
	}

	// artist 거절
	@Transactional
	public void rejectArtist(ArtistResultDTO dto, Long adminId) {
		Approval approval = approvalRepository.findById(dto.getApprovalId())
				.orElseThrow(() -> new IllegalArgumentException("신청서가 없습니다."));

		approval.setStatus("REJECTED");
		approval.setRejectionReason(dto.getRejectionReason());
		approval.setAdminId(adminId);
		approval.setProcessedAt(LocalDateTime.now());

		Member member = memberRepository.findById(approval.getArtistId())
				.orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
		if ("PENDING".equals(member.getStatus())) {
			member.setStatus("ACTIVE");
			log.info("-----> [상태 복구] MemberID: {} 님이 일반 유저(ACTIVE)로 전환되었습니다.", member.getMemberId());
		}

		log.info("-----> [거절 완료] ID: {}, 사유: {}", dto.getApprovalId(), dto.getRejectionReason());
	}

	public void approveBoardReport(Long boardId) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'approveBoardReport'");
	}

	// artist 승인된 리스트
	@Transactional(readOnly = true)
	public List<ArtistResponseDTO> getActiceArtistList(String category, String status) {
		List<Approval> entityList = approvalRepository.findByCategoryAndStatus(category, status);

		return entityList.stream().map(entity -> ArtistResponseDTO.builder().approvalId(entity.getApprovalId())
				.artistId(entity.getArtistId()).artistName(entity.getRequesterName()).category(entity.getSubCategory())
				.status(entity.getStatus()).createdAt(entity.getCreatedAt().toString())
				.processedAt(entity.getProcessedAt() != null ? entity.getProcessedAt().toString() : null)
				.adminId(entity.getAdminId()).build()).collect(Collectors.toList());
	}

	// artist 상세 보기
	public ArtistResponseDTO getArtistDetail(Long approvalId, Long artistId) {

		// 1서버 정보 호출
		Approval approval = approvalRepository.findById(approvalId)
				.orElseThrow(() -> new IllegalArgumentException("해당 기록이 없습니다. ID: " + approvalId));

		Artist artist = artistRepository.findByArtistId(artistId)
				.orElseThrow(() -> new IllegalArgumentException("아티스트를 찾을 수 없습니다. ID: " + artistId));

		Member member = memberRepository.findById(artist.getMemberId())
				.orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다. ID: " + artistId));

		// 2서버 WebClient 호출
		ArtistAccountResponse paymentData = webClient.get().uri(payAdminUrl + "/artist/{artistId}", artistId).retrieve()
				.bodyToMono(ArtistAccountResponse.class)
				.onErrorResume(e -> Mono.just(new ArtistAccountResponse(BigDecimal.ZERO, BigDecimal.ZERO))).block();

		return ArtistResponseDTO.builder().approvalId(approval.getApprovalId()).artistId(member.getMemberId())
				.artistName(member.getName()).category(approval.getCategory()).description(approval.getDescription())
				.createdAt(approval.getCreatedAt().toString()).status(approval.getStatus()).email(member.getEmail())
				.phone(member.getPhone()).followerCount(artist.getFollowerCount())
				.totalBalance(paymentData.getTotalBalance()).withdrawableBalance(paymentData.getWithdrawableBalance())
				.rejectionReason(approval.getRejectionReason()).adminId(approval.getAdminId())
				.processedAt(approval.getProcessedAt() != null ? approval.getProcessedAt().toString() : "처리 대기중")
				.build();

	}


}
