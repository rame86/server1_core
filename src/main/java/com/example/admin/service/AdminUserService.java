package com.example.admin.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.dto.user.UserDetailResponseDTO;
import com.example.admin.dto.user.UserListResponseDTO;
import com.example.admin.dto.user.UserSummaryDTO;
import com.example.artist.dto.PaymentRequestDTO;
import com.example.config.RabbitMQConfig;
import com.example.member.domain.Member;
import com.example.member.domain.MemberHistory;
import com.example.member.repository.MemberHistoryRepository;
import com.example.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

	private final MemberRepository memberRepository;
    private final MemberHistoryRepository memberHistoryRepository;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;
	
    // 유저 목록 조회 (로컬 DB 조회 후 결제 서버에 통계 요청)
	@Transactional(readOnly = true)
	public Page<UserListResponseDTO> getAllUserList(Pageable pageable) {
		// 멤버 페이지 가져오기
		Page<Member> memberPage = memberRepository.findAll(pageable);

		// 현재 페이지에 있는 유저들의 ID만 리스트로 추출
		List<Long> memberId = memberPage.getContent().stream()
				.map(Member::getMemberId)
				.collect(Collectors.toList());
		
		// pay서버에 사용자 통게 데이터 요창
		sendMqRequest("GETALL", memberId);
        sendMqRequest("SUMMARY", memberId);
        
        // 우선은 로컬 DB 정보로 DTO 생성
        return memberPage.map(this::convertToUserListDTO);
	}
	
	// 유저 상세 정보 수색 (로컬 DB + 결제 서버 오청)
	@Transactional(readOnly = true)
	public UserDetailResponseDTO getUserDetail(Long memberId) {
		Member member = findMember(memberId);
		
		// pay서버에 상세 내역 요청
		sendMqRequest("USER_DETAIL", List.of(memberId));
		
		return UserDetailResponseDTO.builder()
				.memberId(member.getMemberId())
				.name(member.getName())
				.email(member.getEmail())
				.status(member.getStatus())
				.phone(member.getPhone())
				.address(member.getAddress())
				.build();
	}
	
	// 공통 MQ요청 발송
	private void sendMqRequest(String orderId, List<Long> memberId) {
		if(memberId == null || memberId.isEmpty()) {
			log.warn("⚠️ [MQ 발송 취소] 대상 memberId 리스트가 비어있습니다. (목적: {})", orderId);
			return;
		}
		
		PaymentRequestDTO.PaymentRequestDTOBuilder builder = PaymentRequestDTO.builder()
                .type("ADMIN")
                .orderId(orderId)
                .replyRoutingKey(RabbitMQConfig.ADMIN_PAY_RES_ROUTING_KEY);
		
		if(memberId.size() == 1) {
			builder.memberId(memberId.get(0));
		} else {
			builder.allMemberId(memberId);
		}
		
		rabbitTemplate.convertAndSend(
				RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.PAY_REQ_ROUTING_KEY,
                builder.build()
		);
		log.info("🚀 [MQ 발송 성공] 목적: {}, 대상 수: {}건", orderId, memberId.size());
	}
	
	// 유저 리스트 DTO 변환
	private UserListResponseDTO convertToUserListDTO(Member member) {
		return UserListResponseDTO.builder()
				.memberId(member.getMemberId())
				.name(member.getName())
				.email(member.getEmail())
				.createdAt(member.getCreatedAt() != null ? member.getCreatedAt().toString() : "날짜 없음")
				.status(member.getStatus()).purchaseCount(0)
				.pointBalance(0L)
				.build();
	}
	
	// 유저 상단 요악 카드
	public UserSummaryDTO getUserSummary() {
		return UserSummaryDTO.builder()
				.totalUserCount(memberRepository.count())
				.activeUserCount(memberRepository.countByStatus("ACTIVE"))
				.blockedUserCount(memberRepository.countByStatus("BLOCK"))
				.build();
	}
    
	// 유저 정지
	@Transactional
	public void blockUser(Long memberId, Long adminId, String reason) {
		findMember(memberId).setStatus("BLOCK");
		saveHistory(memberId, adminId, "BLOCK", reason);
		redisTemplate.opsForValue().set("BLOCK:" + memberId, "true", 24, TimeUnit.HOURS);
		log.info("🚨 [검거 완료] 유저 {} 차단 완료", memberId);
	}

	// 유저 권한 변경
	@Transactional
	public void updateUserRole(Long adminId, Long memberId, String role) {
		Member member = findMember(memberId);
		String oldRole = member.getRole();
		member.setRole(role);
		saveHistory(memberId, adminId, "ACTIVE", "권한 변경: " + oldRole + " -> " + role);
		log.info("✅ 권한 변경 완료 및 히스토리 저장 성공!");
	}

	// 비밀번호 초기화
	@Transactional
	public void resetPassword(Long adminId, Long memberId, String password) {
		findMember(memberId).setPassword(passwordEncoder.encode(password));
		saveHistory(memberId, adminId, "ACTIVE", "관리자에 의한 비밀번호 강제 초기화");
		log.info("✅ 유저 {} 비번 초기화 완료 및 기록 저장!", memberId);
	}

	// 강제 로그아웃
	@Transactional
	public void forceLogout(Long adminId, Long memberId) {
		log.info("📢 [강제 로그아웃 발령] 관리자 {}님이 유저 {}를 쫓아냅니다.", adminId, memberId);
		redisTemplate.opsForValue().set("FORCE_LOGOUT:" + memberId, "true", 1, TimeUnit.HOURS);
		saveHistory(memberId, adminId, "ACTIVE", "관리자에 의한 강제 로그아웃 수행");
	}

	// 유저 탈퇴
	@Transactional
	public void deleteUser(Long adminId, Long memberId) {
		Member member = findMember(memberId);
		member.setStatus("DELETE");
		saveHistory(memberId, adminId, "DELETE", "관리자에 의한 계정 삭제 처리");
		redisTemplate.opsForValue().set("BLOCK:" + memberId, "true", 24, TimeUnit.HOURS);
	}
	
	public Member findMember(Long memberId) {
		return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
	}
	
	private void saveHistory(Long memberId, Long adminId, String status, String reason) {
		memberHistoryRepository.save(MemberHistory.builder()
	            .memberId(memberId)
	            .adminId(adminId)
	            .status(status)
	            .reason(reason)
	            .build());
    }
	
	public List<Map<String, Object>> userGrowthCounts(){
		return memberRepository.getMonthlyUserGrowth();
	}
	
	@Transactional(readOnly = true)
	public Page<UserListResponseDTO> getBlockedUserList(Pageable pageable) {
	    Page<Member> blockedPage = memberRepository.findByStatus("BLOCK", pageable);

	    List<Long> memberIds = blockedPage.getContent().stream()
	            .map(Member::getMemberId)
	            .collect(Collectors.toList());
	    
	    if(!memberIds.isEmpty()) {
	        sendMqRequest("GETALL", memberIds);
	    }

	    return blockedPage.map(this::convertToUserListDTO);
	}

}
