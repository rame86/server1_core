package com.example.admin.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.dto.AdminEventListDTO;
import com.example.admin.dto.AdminRefundResponseDTO;
import com.example.admin.dto.ArtistResponseDTO;
import com.example.admin.dto.ArtistResultDTO;
import com.example.admin.dto.EventResultDTO;
import com.example.admin.dto.SettlementDashboardResponse;
import com.example.admin.dto.ShopResultDTO;
import com.example.admin.dto.UserListResponseDTO;
import com.example.admin.dto.UserManagementPageResponse;
import com.example.admin.dto.UserSummaryDTO;
import com.example.admin.service.AdminRefundService;
import com.example.admin.service.AdminService;
import com.example.common.annotation.LoginUser;
import com.example.config.RabbitMQConfig;
import com.example.member.dto.RedisMemberDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

	private final AdminService adminService;
	private final AdminRefundService adminRefundService;
	
	@PostMapping("/event/confirm")
	public ResponseEntity<String> confirmEvent(@RequestBody EventResultDTO dto, @LoginUser RedisMemberDTO user) {
		log.info("=====> [1서버 관리자] 결정 전송 요청: {}", dto);
		adminService.processApproval(dto, RabbitMQConfig.EVENT_RES_ROUTING_KEY, user.getMemberId());
		return ResponseEntity.ok("EVENT 처리 완료");
	}
	
	@PostMapping("/shop/confirm")
	public ResponseEntity<String> confirmShop(@RequestBody ShopResultDTO dto, @LoginUser RedisMemberDTO user) {
		log.info("=====> [1서버 관리자] 결정 전송 요청: {}", dto);
		adminService.processApproval(dto, RabbitMQConfig.SHOP_RES_ROUTING_KEY, user.getMemberId());
		return ResponseEntity.ok("SHOP 처리 완료");
	}
	
	@PostMapping("/artist/confirm")
	public ResponseEntity<String> confirmArtist(@RequestBody ArtistResultDTO dto, @LoginUser RedisMemberDTO user) {
		log.info("=====> [1서버 관리자] 아티스트 등록 완료");
		adminService.confirmArtist(dto, user.getMemberId());
		return ResponseEntity.ok("ARTIST 처리 완료");
	}
	
	@PostMapping("/artist/reject")
	public ResponseEntity<String> rejectArtist(@RequestBody ArtistResultDTO dto, @LoginUser RedisMemberDTO user) {
		log.info("=====> [1서버 관리자] 아티스트 거절 처리");
		adminService.rejectArtist(dto, user.getMemberId());
		return ResponseEntity.ok("ARTIST 거절 처리 완료");
	}
	
	@GetMapping("/event/list")
	public ResponseEntity<List<AdminEventListDTO>> getEventList() {
		log.info("=====> [1서버 관리자] 전체 이벤트 리스트 조회 요청");
		List<AdminEventListDTO> list = adminService.getAllEvents();
		return ResponseEntity.ok(list);
	}
	
	@PostMapping("/refund")
	public ResponseEntity<String> approveRefund(@RequestBody AdminRefundResponseDTO dto, @LoginUser RedisMemberDTO user) {
		log.info("=====> [1서버 관리자] 결정 전송 요청: {}", dto);
		if(user.getMemberId() != null) dto.setAdminId(user.getMemberId());
		adminRefundService.approveRefund(dto);
		return ResponseEntity.ok("REFUND 처리 완료");
	}
	
	// 승인요청한 아티스트 목록
	@GetMapping("/artist/approvalList")
	public ResponseEntity<List<ArtistResultDTO>> getPendingArtist() {
		List<ArtistResultDTO> pendingList = adminService.getPendingArtistList("ARTIST", "PENDING");
		return ResponseEntity.ok(pendingList);
	}
	
	// 승인된 아티스트 목록
	@GetMapping("/artist/activeList")
	public ResponseEntity<List<ArtistResponseDTO>> getActiveArtist() {
		List<ArtistResponseDTO> activeList = adminService.getActiceArtistList("ARTIST", "CONFIRMED");
		return ResponseEntity.ok(activeList);
	}
	
	// 아티스트 상세 정보
	@GetMapping("/artist/{approvalId}/{artistId}")
	public ResponseEntity<ArtistResponseDTO> getArtistDetail(
			@PathVariable("approvalId") Long approvalId,
			@PathVariable("artistId") Long artistId) {
		return ResponseEntity.ok(adminService.getArtistDetail(approvalId, artistId));
	}
	
	@GetMapping("/settlement")
	public ResponseEntity<SettlementDashboardResponse> getSettlementStats() {
	    log.info("=====> [core 서비스 관리자] 통합 대시보드 데이터 HTTP 요청 발생");
	    SettlementDashboardResponse response = adminService.requestDashboardData();
	    return ResponseEntity.ok(response);
	}
	
	// 유저 통계
	@GetMapping("/user")
	public ResponseEntity<UserManagementPageResponse> getUserList(Pageable pageable) {
		UserSummaryDTO summary = adminService.getUserSummary();
		Page<UserListResponseDTO> userList = adminService.getAllUserList(pageable);
		return ResponseEntity.ok(UserManagementPageResponse.builder()
				.summary(summary)
				.userList(userList)
				.build());
	}

}
