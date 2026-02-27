package com.example.member.service;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.member.dto.OAuthUserInfo;
import com.example.member.entity.Member;
import com.example.member.entity.SocialAccount;
import com.example.member.repository.MemberRepository;
import com.example.member.repository.SocialAccountRepository;
import com.example.security.tokenProvider.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Transactional
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

	private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
	private final SocialAccountRepository socialAccountRepository;
	private final StringRedisTemplate redisTemplate;
    
	@Value("${sign.up.url}")
	private String signUpUrl;
	
	@Value("${login.user.url}")
	private String loginUrl;	

    public void memberLogin(OAuthUserInfo userInfo, HttpServletResponse response) throws IOException {
		// 소셜계정으로 가입된 이력이 있는지 확인하기
		Optional<SocialAccount> socialOpt = socialAccountRepository.findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId());
		// 소셜로 가입/연동된 적이 있음
		if(socialOpt.isPresent()) {
			loginUser(socialOpt.get().getMember(), response);
			return;
		}

		if(userInfo.getEmail() != null && !userInfo.getEmail().trim().isEmpty()) {
			// 소셜 ID는 없지만!! 이메일이 있는 기존회원이 있는지 확인
	    	Optional<Member> memberOpt = memberRepository.findByEmail(userInfo.getEmail());
			if(memberOpt.isPresent()) {
				// 이메일이 있지만 해당 소셜 계정은 없을때
				Member member = memberOpt.get();
				linkSocialAccount(member, userInfo);
				loginUser(member, response);
				return;
			}
		}
		
		// 신규회원
		redirectSignup(userInfo, response);
    }

	// 소셜 계정 연동
	private void linkSocialAccount(Member member, OAuthUserInfo userInfo) {
		SocialAccount social = new SocialAccount();
		social.setMember(member);
		social.setProvider(userInfo.getProvider());
		social.setProviderId(userInfo.getProviderId());
		socialAccountRepository.save(social);
		log.info("---------> [계정 연동] 기존 이메일({})에 소셜({}) 연동 완료", member.getEmail(), userInfo.getProvider());
	}

	// 로그인 처리(JWT 발급 및 이동)
	private void loginUser(Member member, HttpServletResponse response) throws IOException {
		log.info("---------> [로그인 성공] JWT 발급: {}", member.getEmail());
		String jwtToken = jwtTokenProvider.createToken(member.getMemberId(), member.getRole());
		
		// 키 생성 : AUTH:MEMBER:16
		String redisKey = "AUTH:MEMBER:" + member.getMemberId();
		
		// 데이터를 JSON 구조로 만들기
		Map<String, Object> userInfo = new HashMap<>();
		userInfo.put("token", jwtToken);
		userInfo.put("role", member.getRole());
		
		// Jackson ObjectMapper를 사용하여 Map을 JSON 문자열로 변환
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonUserInfo = objectMapper.writeValueAsString(userInfo);
		log.info("JSON으로 저장될 값: {}", jsonUserInfo);

		// Redis에 저장
	    redisTemplate.opsForValue().set(redisKey, jsonUserInfo, Duration.ofHours(1));
	    
	    String redirectUrl = loginUrl + jwtToken 
	    		+ "&member_id=" + member.getMemberId()
	    		+ "&role=" + member.getRole();
		
	    response.sendRedirect(redirectUrl);
	}

	// 회원가입 페이지로 이동
	private void redirectSignup(OAuthUserInfo userInfo, HttpServletResponse response) throws IOException {
		log.info("---------> [신규 유저] 추가 정보 입력 페이지로 이동");

		String email = (userInfo.getEmail() != null) ? userInfo.getEmail() : "";
		String nickName = "";
		if(userInfo.getNickname() != null) nickName = java.net.URLEncoder.encode(userInfo.getNickname(), java.nio.charset.StandardCharsets.UTF_8);

    	String redirectUrl = signUpUrl
        		+ "?email=" + email
        		+ "&nickname=" + nickName
        		+ "&provider=" + userInfo.getProvider()
    			+ "&providerId=" + userInfo.getProviderId();
       	response.sendRedirect(redirectUrl);
	}

}
