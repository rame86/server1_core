package com.example.member.controller;

import java.time.Duration;
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
import com.example.security.JwtTokenProvider;

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
			    
				String token = jwtTokenProvider.createToken(member.getMemberId(), member.getRole());
				return ResponseEntity.ok(Map.of(
					"message", "기존 계정에 소셜 정보가 연동되었습니다!",
					"token", token,
					"redirectUrl", "/"
	        	));
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
		member.setPassword(passwordEncoder.encode(dto.getPassword()));

		// 소셜계정 생성
		SocialAccount socialAccount = new SocialAccount();
		socialAccount.setProvider(dto.getProvider());
		socialAccount.setProviderId(dto.getProviderId());
		socialAccount.setMember(member); // 연관관계 설정

		// Member의 리스트에도 추가
		member.getSocialAccounts().add(socialAccount);

    	Member savedMember = memberRepository.save(member);
    	String token = jwtTokenProvider.createToken(savedMember.getMemberId(), savedMember.getRole());
    	
    	log.info("인증 완료로 인한 Redis 키 삭제: {}", dto.getEmail());
    	redisTemplate.delete("CHECK:" + dto.getEmail());
    	
    	return ResponseEntity.ok(Map.of(
    		    "message", "회원가입 성공! 환영합니다.",
    		    "token", token,
    		    "redirectUrl", "/"
    	));
    	
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

}
