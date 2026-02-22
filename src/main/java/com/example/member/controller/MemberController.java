package com.example.member.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.member.dto.MemberSignupRequest;
import com.example.member.entity.Member;
import com.example.member.entity.SocialAccount;
import com.example.member.repository.MemberRepository;
import com.example.member.repository.SocialAccountRepository;
import com.example.security.JwtTokenProvider;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequestMapping("/member")
@RequiredArgsConstructor
@RestController
@Transactional
public class MemberController {
	
	private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
	private final SocialAccountRepository socialAccountRepository;
     
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody MemberSignupRequest dto) {

		Optional<Member> existingMember = memberRepository.findByEmail(dto.getEmail());
    	
		// 기존 회원이 있는 경우
		if(existingMember.isPresent()) {
			Member member = existingMember.get();

			SocialAccount socialAccount = new SocialAccount();
        	socialAccount.setProvider(dto.getProvider());
        	socialAccount.setProviderId(dto.getProviderId());
        	socialAccount.setMember(member);

			socialAccountRepository.save(socialAccount);
			
			String token = jwtTokenProvider.createToken(member.getMemberId().toString());
			return ResponseEntity.ok(Map.of(
				"message", "기존 계정에 소셜 정보가 연동되었습니다!",
				"token", token,
				"redirectUrl", "/"
        	));
		}

		// 회원생성
    	Member member = new Member();
    	member.setEmail(dto.getEmail());
    	member.setName(dto.getName());
    	member.setPhone(dto.getPhone());
    	member.setAddress(dto.getAddress());
		member.setAge(dto.getAge());
		member.setPassword(dto.getPassword());

		// 소셜계정 생성
		SocialAccount socialAccount = new SocialAccount();
		socialAccount.setProvider(dto.getProvider());
		socialAccount.setProviderId(dto.getProviderId());
		socialAccount.setMember(member); // 연관관계 설정

		// Member의 리스트에도 추가
		member.getSocialAccounts().add(socialAccount);

    	Member savedMember = memberRepository.save(member);
    	String token = jwtTokenProvider.createToken(savedMember.getMemberId().toString());
    	
    	return ResponseEntity.ok(Map.of(
    		    "message", "회원가입 성공! 환영합니다.",
    		    "token", token,
    		    "redirectUrl", "/"
    	));
    	
    }

}
