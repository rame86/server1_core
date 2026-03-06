package com.example.artist.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString 
public class PaymentRequestDTO {

    private String orderId;
    private Long memberId;
    private BigDecimal amount;
    private String type;
    private String eventTitle;

    // 요청한 서비스가 응답받길 원하는 라우팅 키
    // 이거 진짜진짜 중요하다고...ㅠ
    // ex) "res.status.update" 또는 "shop.status.update"
    private String replyRoutingKey;
    
    private BigDecimal chargeAmount;
    private String payType;

}
