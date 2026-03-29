package com.example.admin.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.dto.user.UserDetailResponseDTO;
import com.example.admin.dto.user.UserListResponseDTO;
import com.example.admin.dto.user.UserManagementPageResponse;
import com.example.admin.dto.user.UserSummaryDTO;
import com.example.admin.service.AdminUserService;
import com.example.common.annotation.LoginUser;
import com.example.member.dto.RedisMemberDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

	private final AdminUserService adminUserService;
	
	// 유저 목록 및 통계
	@GetMapping
	public ResponseEntity<UserManagementPageResponse> getUserList(Pageable pageable) {
		UserSummaryDTO summary = adminUserService.getUserSummary();
		Page<UserListResponseDTO> userList = adminUserService.getAllUserList(pageable);
		return ResponseEntity.ok(UserManagementPageResponse.builder()
				.summary(summary)
				.userList(userList)
				.build());
	}
	
	// 유저 상세정보
	@GetMapping("/{memberId}")
	public ResponseEntity<UserDetailResponseDTO> getUserDetail(@PathVariable("memberId") Long memberId) {
		log.info("=====> [AdminController] 유저 상세 수색 요청 수신! ID: {}", memberId);
		UserDetailResponseDTO detail = adminUserService.getUserDetail(memberId);
		return ResponseEntity.ok(detail);
	}
	
	// 유저 차단
	@PostMapping("/block")
	public ResponseEntity<String> blockUser(@LoginUser RedisMemberDTO user, @RequestBody Map<String, Object> params) {
		Long memberId = Long.parseLong(params.get("memberId").toString());
		String reason = params.get("reason").toString();
		
		log.info("🚨 [AdminController] 관리자 {}님이 유저 {}를 차단합니다. 사유: {}", user.getMemberId(), memberId, reason);
		
		adminUserService.blockUser(memberId, user.getMemberId(), reason);
		return ResponseEntity.ok("유저 차단 및 강제 로그아웃 처리가 완료되었습니다.");
	}
	
	// 유저 권한 변경(USER, ARTIST, ADMIN)
	@PostMapping("/role")
	public ResponseEntity<String> updateUserRole(@LoginUser RedisMemberDTO user, @RequestBody Map<String, Object> params) {
		Long memberId = Long.parseLong(params.get("memberId").toString());
		String newRole = params.get("role").toString();
		
		log.info("👮‍♂️ [권한 변경] 유저 ID: {} -> 새로운 권한: {}", memberId, newRole);
		adminUserService.updateUserRole(user.getMemberId(), memberId, newRole);
		return ResponseEntity.ok("권한 변경이 완료되었습니다.");
	}
	
	// 유저 비밀번호 초기화
	@PostMapping("/resetPwd")
	public ResponseEntity<String> resetPassword(@LoginUser RedisMemberDTO user, @RequestBody Map<String, Object> params) {
		Long memberId = Long.parseLong(params.get("memberId").toString());
	    String password = params.get("password").toString();
	    log.info("🔐 [비번 초기화] 관리자 {}님이 유저 {}의 비번을 변경합니다.", user.getMemberId(), memberId);
	    adminUserService.resetPassword(user.getMemberId(), memberId, password);
	    return ResponseEntity.ok("비밀번호가 성공적으로 초기화되었습니다.");
	}
	
	// 강제 로그아웃
	@PostMapping("/logout")
	public ResponseEntity<String> forceLogout(@RequestBody Map<String, Long> params, @LoginUser RedisMemberDTO user) {
		adminUserService.forceLogout(user.getMemberId(), params.get("memberId"));
	    return ResponseEntity.ok("강제 로그아웃 처리가 완료되었습니다.");
	}
	
	// 계정 삭제
	@PostMapping("/delete")
	public ResponseEntity<String> deleteUser(@RequestBody Map<String, Long> params, @LoginUser RedisMemberDTO user) {
		adminUserService.deleteUser(user.getMemberId(), params.get("memberId"));
	    return ResponseEntity.ok("계정이 삭제 처리되었습니다.");
	}
	
	@GetMapping("/block/list")
	public ResponseEntity<Page<UserListResponseDTO>> getBlockedUsers(Pageable pageable) {
	    log.info("🚨 [AdminController] 차단 유저 명단 수색 개시!");
	    Page<UserListResponseDTO> blockedList = adminUserService.getBlockedUserList(pageable);
	    return ResponseEntity.ok(blockedList);
	}
	
}
