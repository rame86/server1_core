package com.example.member.service;

import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.member.dto.OAuthUserInfo;
import com.example.member.entity.Member;
import com.example.member.repository.MemberRepository;
import com.example.security.JwtTokenProvider;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {
	
	private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    
    public void memberLogin(OAuthUserInfo userInfo, HttpServletResponse response) throws IOException {
    	
    	// DB에서 해당 이메일로 가입된 회원이 있는지 확인
    	Optional<Member> memberOpt = memberRepository.findByEmail(userInfo.getEmail());
    	
    	if(memberOpt.isEmpty()) {
    		// 신규유저 -> 회원가입 창으로 리다이랙트
    		log.info("---------> [신규 유저] 추가 정보 입력 페이지로 이동");
    		String redirectUrl = "http://localhost:3000/extra-info"
        			+ "?email=" + userInfo.getEmail()
        			+ "&nickname=" + userInfo.getNickname()
        			+ "&provider=" + userInfo.getProvider()
        			+ "&providerId=" + userInfo.getProviderId();
        	response.sendRedirect(redirectUrl);
    	} else {
    		// 기존유저 -> JWT발급 후 메인으로 리다이랙트
    		log.info("---------> [로그인 성공] JWT 발급 후 메인으로 이동");
    		String jwtToken = jwtTokenProvider.createToken(memberOpt.get().getMemberId().toString());
    		response.sendRedirect("http://localhost:5173/?token=" + jwtToken);
    	}
    	
    }

}
