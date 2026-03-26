package com.example.artist.dto;

import java.math.BigDecimal;
import java.util.List;

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
    private Long artistId;
    private String replyRoutingKey;
    
    private List<Long> allMemberId; 
    private List<Long> allArtistId;
    
    private BigDecimal chargeAmount;
    private String payType;

}
