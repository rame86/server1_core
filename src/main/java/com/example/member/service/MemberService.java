package com.example.member.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.member.dto.MemberSignupRequest;
import com.example.member.entity.Member;
import com.example.member.entity.SocialAccount;
import com.example.member.repository.MemberRepository;
import com.example.member.repository.SocialAccountRepository;
import com.example.security.tokenProvider.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
	
	// 회원가입 및 소셜 연동
	public Map<String, Object> registerMember(MemberSignupRequest dto) {
		// 인증번호 검증
		if(!mailSenderService.verifyCode(dto.getEmail(), dto.getAuthCode())) {
			throw new IllegalArgumentException("인증번호가 일치하지 않거나 만료되었습니다.");
		}
		
		Optional<Member> existingMember = memberRepository.findByEmail(dto.getEmail());
		Member member;
		
		// 기존 회원이 있는 경우
		if(existingMember.isPresent()) {
			member = handleExistingMember(existingMember.get(), dto);
		} else {
			member = createNewMember(dto);
		}
		
		// 인증 완료 후 Redis 키 삭제
		mailSenderService.deleteVerificationCode(dto.getEmail());
		
		return loginResponse(member, "회원가입 및 처리가 완료되었습니다."); 
	}
	
	// 이미 있는 계정일 경우
	private Member handleExistingMember(Member member, MemberSignupRequest dto) {
		// 소셜 계정 연동일 경우
		if(dto.getProvider() != null && !dto.getProvider().isEmpty()) {
			Optional<SocialAccount> alreadyLinked = socialAccountRepository.findByProviderAndProviderId(dto.getProvider(), dto.getProviderId());
			
			if(alreadyLinked.isEmpty()) {
				SocialAccount socialAccount = new SocialAccount();
	        	socialAccount.setProvider(dto.getProvider());
	        	socialAccount.setProviderId(dto.getProviderId());
	        	socialAccount.setMember(member);
	        	
	        	socialAccountRepository.save(socialAccount);
			}
			return member;
		}
		
		// 소셜 유저가 일반 회원가입을 할 경우
		if(passwordEncoder.matches("SOCIAL_USER", member.getPassword())) {
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
		if(dto.getProvider() != null && !dto.getProvider().isEmpty()) {
			member.setPassword(passwordEncoder.encode("SOCIAL_USER"));
			
			SocialAccount socialAccount = new SocialAccount();
			socialAccount.setProvider(dto.getProvider());
			socialAccount.setProviderId(dto.getProviderId());
			socialAccount.setMember(member); // 연관관계 설정

			member.getSocialAccounts().add(socialAccount);	
			memberRepository.save(member);
			socialAccountRepository.save(socialAccount);

			log.info("-----> 소셜 계정 테이블 저장 완료: {}", dto.getProvider());
		}else {
			member.setPassword(passwordEncoder.encode(dto.getPassword()));
			memberRepository.save(member);
		}
		return member;
	}
		
	// 로그인 성공 시 토큰 발급 및 redis 저장
	public Map<String, Object> loginResponse(Member member, String message) {
		log.info("---------> [로그인 성공] JWT 발급: {}", member.getEmail());
    	String jwtToken = jwtTokenProvider.createToken(member.getMemberId(), member.getRole());
    	
    	String redisKey = "AUTH:MEMBER:" + member.getMemberId();
    	
    	// 데이터를 JSON 구조로 만들기
    	Map<String, Object> userInfo = new HashMap<>();
    	userInfo.put("token", jwtToken);
		userInfo.put("role", member.getRole());
		userInfo.put("last_login", java.time.LocalDateTime.now().toString());
    	
    	try {
    		// Jackson ObjectMapper를 사용하여 Map을 JSON 문자열로 변환
        	ObjectMapper objectMapper = new ObjectMapper();
        	String jsonUserInfo = objectMapper.writeValueAsString(userInfo);
        	log.info("JSON으로 저장될 값: {}", jsonUserInfo);
        	
        	// Redis에 저장
    	    redisTemplate.opsForValue().set(redisKey, jsonUserInfo, Duration.ofHours(1));
    	} catch(Exception e) {
    		log.error("Redis 저장용 JSON 변환 실패", e);
    	}
    	
    	return Map.of(
    		"message", message,
            "token", jwtToken,
            "member_id", member.getMemberId(),
            "role", member.getRole(),
            "redirectUrl", "/"
    	);
	}
	
	// 일반 로그인
	public Map<String, Object> login(String email, String password) {
		Member member = memberRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));
		
		if(!passwordEncoder.matches(password, member.getPassword())) {
			throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
		}
		
		return loginResponse(member, "로그인 성공!");
	}
	
	// 로그 아웃
	public void logout(String token) {
		String redisKey = "TOKEN:" + token;
		if(Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
			redisTemplate.delete(redisKey);
			log.info("---------> [로그아웃] Redis에서 토큰 삭제 완료: {}", token);
		} else {
			log.warn("---------> [로그아웃] 이미 만료되었거나 존재하지 않는 토큰입니다.");
		}
	}

}
