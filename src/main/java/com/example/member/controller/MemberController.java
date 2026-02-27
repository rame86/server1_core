package com.example.member.controller;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.member.dto.MemberSignupRequest;
import com.example.member.entity.Member;
import com.example.member.entity.SocialAccount;
import com.example.member.repository.MemberRepository;
import com.example.member.repository.SocialAccountRepository;
import com.example.member.service.MailSenderService;
import com.example.security.tokenProvider.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/member")
@RequiredArgsConstructor
@RestController
@Transactional
@Slf4j
public class MemberController {
	
	private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
	private final SocialAccountRepository socialAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final MailSenderService mailSenderService;
	private final StringRedisTemplate redisTemplate;
     
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody MemberSignupRequest dto) {

    	// Redis에서 해당 이메일의 인증번호 가져오기
    	String saveCode = redisTemplate.opsForValue().get("CHECK:" + dto.getEmail());
    	
    	// 인증번호가 없거나(시간초과) 입력한 번호와 다르면 에러!
    	if(saveCode == null || !saveCode.equals(dto.getAuthCode())) {
    		return ResponseEntity.badRequest().body(Map.of("message", "인증번호가 일치하지 않거나 만료되었습니다."));
    	}
    	
		Optional<Member> existingMember = memberRepository.findByEmail(dto.getEmail());
		
		// 기존 회원이 있는 경우
		if(existingMember.isPresent()) {
			Member member = existingMember.get();
			
			if(dto.getProvider() != null && !dto.getProviderId().isEmpty()) {
				
				// 소셜 계정이 이미 이 회원에게 연결되어 있는지 최종적으로 확인
				Optional<SocialAccount> alreadyLinked = socialAccountRepository.findByProviderAndProviderId(dto.getProvider(), dto.getProviderId());

				if(alreadyLinked.isEmpty()) {
					SocialAccount socialAccount = new SocialAccount();
		        	socialAccount.setProvider(dto.getProvider());
		        	socialAccount.setProviderId(dto.getProviderId());
		        	socialAccount.setMember(member);
		        	
		        	socialAccountRepository.save(socialAccount);
				}
				
				log.info("인증 완료로 인한 Redis 키 삭제: {}", dto.getEmail());
				redisTemplate.delete("CHECK:" + dto.getEmail());

				return loginUser(member, "기존 계정에 소셜 정보가 연동되었습니다!");
				
			} else if(passwordEncoder.matches("SOCIAL_USER", member.getPassword())) {
				
				// 소셜 유저가 비밀번호 설정함
				member.setPassword(passwordEncoder.encode(dto.getPassword()));
				member.setName(dto.getName());
				member.setPhone(dto.getPhone());
				memberRepository.save(member);
				
				log.info("인증 완료로 인한 Redis 키 삭제: {}", dto.getEmail());
				redisTemplate.delete("CHECK:" + dto.getEmail());
				
				return loginUser(member, "일반 로그인 설정이 완료되었습니다!");
				
			} else {
				return ResponseEntity.badRequest().body(Map.of("message", "이미 가입된 이메일입니다."));
			}
		}

		// 회원생성
    	Member member = new Member();
    	member.setEmail(dto.getEmail());
    	member.setName(dto.getName());
    	member.setPhone(dto.getPhone());
    	member.setAddress(dto.getAddress());
		member.setAge(dto.getAge());
		member.setRole("USER");
		
		// 소셜계정 생성
		if(dto.getProvider() != null && !dto.getProvider().isEmpty()) {
			
			SocialAccount socialAccount = new SocialAccount();
			socialAccount.setProvider(dto.getProvider());
			socialAccount.setProviderId(dto.getProviderId());
			socialAccount.setMember(member); // 연관관계 설정

			member.getSocialAccounts().add(socialAccount);	
			member.setPassword(passwordEncoder.encode("SOCIAL_USER"));
			
			Member savedMember = memberRepository.save(member);
			socialAccountRepository.save(socialAccount);
			
			log.info("-----> 소셜 계정 테이블 저장 완료: {}", dto.getProvider());
	    	redisTemplate.delete("CHECK:" + dto.getEmail());
	    	return loginUser(savedMember, "회원가입 성공! 환영합니다.");
			
		}else {
			member.setPassword(passwordEncoder.encode(dto.getPassword()));
	    	Member savedMember = memberRepository.save(member);
	    	
	    	log.info("인증 완료로 인한 Redis 키 삭제: {}", dto.getEmail());
	    	redisTemplate.delete("CHECK:" + dto.getEmail());
	    	return loginUser(savedMember, "회원가입 성공! 환영합니다.");
		}
    	
    }
    
    // 인증메일
    @PostMapping("/SendVerification")
    public ResponseEntity<?> sendVerificationCode(@RequestParam("email")String email){
    	// 1. 6자리 랜덤 인증번호 생성 (100000 ~ 999999)
    	String verificationCode = String.valueOf((int)(Math.random() * 899999) + 100000);
    	
    	// 2. Redis에 저장하기(key : email, Value : 인증번호, 유효시간 : 5분)
    	redisTemplate.opsForValue().set("CHECK:" + email, verificationCode, Duration.ofMinutes(5));
    	log.info("-----> [Redis 저장] 이메일: {}, 인증번호: {}", email, verificationCode);
    	        
        // 3. 메일 발송
        String title = "[TEST] 회원가입 인증번호입니다.";
        String content = "안녕하세요! 인증번호는 [" + verificationCode + "] 입니다. \n5분 이내에 입력해 주세요.";
    	
        try {
        	mailSenderService.sendEmail(email, title, content);
            return ResponseEntity.ok(Map.of("message", "인증번호가 발송되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "메일 발송 실패: " + e.getMessage()));
        }
    }
    
    // 토큰을 만들고 redis에 저장한 뒤 응답 데이터 반환(로그인 처리) -> 우리사이트(?) 회원
    private ResponseEntity<?> loginUser(Member member, String message) {
    	log.info("---------> [로그인 성공] JWT 발급: {}", member.getEmail());
    	String jwtToken = jwtTokenProvider.createToken(member.getMemberId(), member.getRole());
    	
    	// 데이터를 JSON 구조로 만들기
    	Map<String, Object> userInfo = new HashMap<>();
    	userInfo.put("member_id", member.getMemberId());
    	userInfo.put("role", member.getRole());
    	
    	try {
    		// Jackson ObjectMapper를 사용하여 Map을 JSON 문자열로 변환
        	ObjectMapper objectMapper = new ObjectMapper();
        	String jsonUserInfo = objectMapper.writeValueAsString(userInfo);
        	log.info("JSON으로 저장될 값: {}", jsonUserInfo);
        	
        	// Redis에 저장
    	    redisTemplate.opsForValue().set("TOKEN:" + jwtToken, jsonUserInfo, Duration.ofHours(1));
    	} catch(Exception e) {
    		log.error("Redis 저장용 JSON 변환 실패", e);
    	}
    	    	
    	return ResponseEntity.ok(Map.of(
                "message", message,
                "token", jwtToken,
                "member_id", member.getMemberId(),
                "role", member.getRole(),
                "redirectUrl", "/"
    	));
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
    	
    	String email = loginData.get("email");
    	String password = loginData.get("password");
    	
    	Optional<Member> memberOpt = memberRepository.findByEmail(email);
    	if(memberOpt.isEmpty()) {
    		return ResponseEntity.badRequest().body(Map.of("message", "존재하지 않는 계정입니다."));
    	}
    	
    	Member member = memberOpt.get();
    	if(!passwordEncoder.matches(password, member.getPassword())) {
    		return ResponseEntity.badRequest().body(Map.of("message", "비밀번호가 일치하지 않습니다."));
    	}
    	
    	return loginUser(member, "로그인 성공!");
    	
    }

}
