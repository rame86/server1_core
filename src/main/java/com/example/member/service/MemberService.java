package com.example.member.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.member.domain.Member;
import com.example.member.domain.SocialAccount;
import com.example.member.dto.MemberSignupRequest;
import com.example.member.repository.MemberRepository;
import com.example.member.repository.SocialAccountRepository;
import com.example.security.tokenProvider.JwtTokenProvider;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Transactional
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final SocialAccountRepository socialAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final StringRedisTemplate redisTemplate;
	private final MailSenderService mailSenderService;
	private final WebClient webClient;

	@Value("${pay.url}")
	private String paymentUrl;

	// 회원가입 및 소셜 연동
	public Map<String, Object> registerMember(MemberSignupRequest dto, HttpServletResponse response) {
		// 인증번호 검증
		if (!mailSenderService.verifyCode(dto.getEmail(), dto.getAuthCode())) {
			throw new IllegalArgumentException("인증번호가 일치하지 않거나 만료되었습니다.");
		}

		Optional<Member> existingMember = memberRepository.findByEmail(dto.getEmail());
		Member member;

		// 기존 회원이 있는 경우
		if (existingMember.isPresent()) {
			member = handleExistingMember(existingMember.get(), dto);
		} else {
			member = createNewMember(dto);
		}

		// 인증 완료 후 Redis 키 삭제
		mailSenderService.deleteVerificationCode(dto.getEmail());

		return loginResponse(member, "회원가입 및 처리가 완료되었습니다.", response);
	}

	// 이미 있는 계정일 경우
	private Member handleExistingMember(Member member, MemberSignupRequest dto) {
		// 소셜 계정 연동일 경우
		if (dto.getProvider() != null && !dto.getProvider().isEmpty()) {
			Optional<SocialAccount> alreadyLinked = socialAccountRepository
					.findByProviderAndProviderId(dto.getProvider(), dto.getProviderId());

			if (alreadyLinked.isEmpty()) {
				SocialAccount socialAccount = new SocialAccount();
				socialAccount.setProvider(dto.getProvider());
				socialAccount.setProviderId(dto.getProviderId());
				socialAccount.setMember(member);

				socialAccountRepository.save(socialAccount);
			}
			return member;
		}

		// 소셜 유저가 일반 회원가입을 할 경우
		if (passwordEncoder.matches("SOCIAL_USER", member.getPassword())) {
			member.setPassword(passwordEncoder.encode(dto.getPassword()));
			member.setName(dto.getName());
			member.setPhone(dto.getPhone());

			return memberRepository.save(member);
		}

		throw new IllegalStateException("이미 가입된 이메일입니다.");
	}

	// 새로운 회원
	private Member createNewMember(MemberSignupRequest dto) {
		Member member = new Member();
		member.setEmail(dto.getEmail());
		member.setName(dto.getName());
		member.setPhone(dto.getPhone());
		member.setAddress(dto.getAddress());
		member.setAge(dto.getAge());
		member.setRole("USER");

		// 소셜계정 생성
		if (dto.getProvider() != null && !dto.getProvider().isEmpty()) {
			member.setPassword(passwordEncoder.encode("SOCIAL_USER"));

			SocialAccount socialAccount = new SocialAccount();
			socialAccount.setProvider(dto.getProvider());
			socialAccount.setProviderId(dto.getProviderId());
			socialAccount.setMember(member); // 연관관계 설정

			member.getSocialAccounts().add(socialAccount);
			memberRepository.save(member);
			socialAccountRepository.save(socialAccount);

			log.info("-----> 소셜 계정 테이블 저장 완료: {}", dto.getProvider());
		} else {
			member.setPassword(passwordEncoder.encode(dto.getPassword()));
			memberRepository.save(member);
		}
		return member;
	}

	// 로그인 성공 시 토큰 발급 및 redis 저장
	public Map<String, Object> loginResponse(Member member, String message, HttpServletResponse response) {
		log.info("---------> [로그인 성공] JWT 및 리프레시 토큰 발급: {}", member.getEmail());

		// 토큰 2개 생성
		String jwtToken = jwtTokenProvider.createToken(member.getMemberId(), member.getRole());
		String refreshToken = jwtTokenProvider.refreshToken(member.getMemberId(), member.getRole());

		// Redis용 키 설정
		String redisKey = "AUTH:MEMBER:" + member.getMemberId(); // 유저 상세 정보용 (Hash)
		String refreshKey = "AUTH:REFRESH:" + member.getMemberId(); // 리프레시 토큰 전용 (String)

		// 결제 정보 조회 (WebClient)
		Long balance = webClient.get()
				.uri(paymentUrl + member.getMemberId())
				.retrieve()
				.bodyToMono(Long.class)
				.onErrorResume(e -> {
					log.error("Payment 서버 조회 실패! ID: {}", member.getMemberId());
					return Mono.just(0L);
				}).block();

		// Redis 유저 정보 Hash 저장 (token 포함)
		Map<String, String> userInfo = new HashMap<>();
		userInfo.put("token", jwtToken);
		userInfo.put("role", member.getRole());
		userInfo.put("balance", String.valueOf(balance));

		redisTemplate.opsForHash().putAll(redisKey, userInfo);
		redisTemplate.expire(redisKey, Duration.ofHours(1));

		// Redis 리프레시 토큰 저장
		redisTemplate.opsForValue().set(refreshKey, refreshToken, Duration.ofDays(14));

		// 리프레시 토큰을 보안 쿠키에 담기
		ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
				.httpOnly(true) // 자바스크립트로 접근 불가 (XSS 방어)
				.secure(false) // HTTPS에서만 작동 (로컬 테스트 시 false로 설정 가능)
				.path("/") // 모든 경로에서 쿠키 사용 가능
				.maxAge(14 * 24 * 60 * 60) // 14일 유지
				.sameSite("Lax") // CSRF 공격 방지
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		log.info("*************************** local 저장 정보 member_id:{} role:{} name:{}",member.getMemberId(),member.getRole(),member.getName());
		// 로컬 스토리지에 담을 정보들
		return Map.of(
				"message", message,
				"token", jwtToken, // 클라이언트는 이걸로 API 호출
				"member_id", member.getMemberId(),
				"role", member.getRole(),
				"name", member.getName(),
				"payment", Map.of("balance", balance));
	}

	// 리프레시 토큰
	public Map<String, Object> refreshToken(String refreshToken, HttpServletResponse response) {
		// 1. refresh 토큰이 유효한지 확인
		if (!jwtTokenProvider.validateToken(refreshToken)) {
			throw new IllegalArgumentException("리프레시 토큰이 만료되었습니다. 다시 로그인해주세요.");
		}

		// 2. 토큰에서 유저 id 추출
		String memberId = jwtTokenProvider.getSubject(refreshToken);

		// 3. redis에 저장된 refresh토큰과 일치하는지 확인
		String redisKey = "AUTH:REFRESH:" + memberId;
		String savedToken = redisTemplate.opsForValue().get(redisKey);

		if (savedToken == null || !savedToken.equals(refreshToken)) {
			throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
		}

		// 4. 최신 유저 정보 조회
		Member member = memberRepository.findById(Long.parseLong(memberId))
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

		// 5. 기존 loginResponse를 사용해 토큰 재발급하기
		log.info("---------> [토큰 재발급] 유저 ID: {} 의 새로운 토큰을 발급합니다.", memberId);
		return loginResponse(member, "토큰 재발급 성공", response);
	}

	// 일반 로그인
	public Map<String, Object> login(String email, String password, HttpServletResponse response) {
		Member member = memberRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

		if (!passwordEncoder.matches(password, member.getPassword())) {
			throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
		}

		return loginResponse(member, "로그인 성공!", response);
	}

	// 로그 아웃
	public void logout(String token, HttpServletResponse response) {
		// 토큰에서 memberId추출
		String memberId = jwtTokenProvider.getSubject(token);

		// 삭제할 키 정의
		String redisKey = "AUTH:MEMBER:" + memberId;
		String refreshKey = "AUTH:REFRESH:" + memberId;

		// 유저 정보 삭제
		if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
			redisTemplate.delete(redisKey);
			log.info("---------> [로그아웃] Redis에서 정보 삭제 완료(유저 ID: {})", memberId);
		} else {
			log.warn("---------> [로그아웃] 이미 만료되었거나 존재하지 않는 토큰입니다.");
		}

		// 리프레시 토큰 삭제
		if (Boolean.TRUE.equals(redisTemplate.hasKey(refreshKey))) {
			redisTemplate.delete(refreshKey);
			log.info("---------> [로그아웃] Redis 리프레시 토큰 삭제 완료 (ID: {})", memberId);
		} else {
			log.warn("---------> [로그아웃] 리프레시 토큰이 이미 없거나 만료되었습니다.");
		}

		// 쿠키에서 리프레시토큰 삭제
		ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
				.httpOnly(true)
				.secure(false)
				.path("/")
				.maxAge(0)
				.sameSite("Lax")
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		log.info("---------> [로그아웃] 브라우저 쿠키 삭제 명령 전송 완료");
	}

}
