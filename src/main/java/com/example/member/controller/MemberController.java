package com.example.member.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.member.dto.MemberSignupRequest;
import com.example.member.entity.Member;
import com.example.member.repository.MemberRepository;
import com.example.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@RequestMapping("/member")
@RequiredArgsConstructor
@RestController
public class MemberController {
	
	private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
     
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody MemberSignupRequest dto) {
    	
    	Map<String, Object> userInfo = new HashMap<>();
    	userInfo.put(dto.getProvider(), dto.getInfo());
    	
    	Member member = new Member();
    	member.setEmail(dto.getEmail());
    	member.setName(dto.getName());
    	member.setPhone(dto.getPhone());
    	member.setAddress(dto.getAddress());
    	member.setInfo(userInfo);
    	
    	Member savedMember = memberRepository.save(member);
    	String token = jwtTokenProvider.createToken(savedMember.getMemberId().toString());
    	
    	return ResponseEntity.ok(Map.of(
    		    "message", "회원가입 성공! 환영합니다.",
    		    "token", token,
    		    "redirectUrl", "/"
    		));
    	
    }

}
