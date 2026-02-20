package com.example.member.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // 파라미터가 없는 기본 생성자(JSON을위해 사용)
public class GoogleUserInfoResponse implements OAuthUserInfo {
	
	private String sub;          
    private String name;         
    private String given_name;   
    private String family_name;  
    private String picture;      
    private String email;        
    private boolean email_verified;
    private String locale;
    
	@Override
	public String getProviderId() { return getSub(); }
	@Override
	public String getNickname() { return getName(); }
	@Override
	public String getProvider() { return "google"; }
}
