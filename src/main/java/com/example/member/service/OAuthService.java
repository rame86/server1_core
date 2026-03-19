package com.example.member.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.member.domain.Member;
import com.example.member.domain.SocialAccount;
import com.example.member.dto.OAuthUserInfo;
import com.example.member.repository.MemberRepository;
import com.example.member.repository.SocialAccountRepository;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Transactional
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final MemberService memberService;
	private final MemberRepository memberRepository;
	private final SocialAccountRepository socialAccountRepository;

    public Map<String, Object> memberLogin(OAuthUserInfo userInfo, HttpServletResponse response) {
    	
		// 소셜계정으로 가입된 이력이 있는지 확인하기
		Optional<SocialAccount> socialOpt = socialAccountRepository.findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId());
		
		// 소셜로 가입/연동된 적이 있음
		if(socialOpt.isPresent()) {
			Member member = socialOpt.get().getMember();
			if("BLOCK".equals(member.getStatus())) {
				throw new IllegalArgumentException("정지된 계정입니다. 고객센터에 문의하세요.");
			}
			return memberService.loginResponse(socialOpt.get().getMember(), "소셜 로그인 성공", response);
		}

		if(userInfo.getEmail() != null && !userInfo.getEmail().trim().isEmpty()) {
			// 소셜 ID는 없지만!! 이메일이 있는 기존회원이 있는지 확인
	    	Optional<Member> memberOpt = memberRepository.findByEmail(userInfo.getEmail());
			if(memberOpt.isPresent()) {
				// 이메일이 있지만 해당 소셜 계정은 없을때
				Member member = memberOpt.get();
				if("BLOCK".equals(member.getStatus())) {
					throw new IllegalArgumentException("정지된 계정입니다. 고객센터에 문의하세요.");
				}
				linkSocialAccount(member, userInfo);
				return memberService.loginResponse(member, "연동 성공", response);
			}
		}
		
		// 신규회원
		return Map.of("status", "signup", "userInfo", userInfo);
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

}
