package com.example.member.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GoogleTokenResponse {
	private String sub;          
    private String name;         
    private String given_name;   
    private String family_name;  
    private String picture;      
    private String email;        
    private boolean email_verified;
    private String locale;
}
