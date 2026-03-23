package com.example.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MemberUpdateRequestDTO {
	private String name;
	private String phone;
	private String nickName;
	private String age;
	private String address;
}
