package com.auction.back.domain.auction.dto.ws;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
public class BidMessage {
    private Long auctionId;    // 어느 경매인지 (또는 PathVariable로도 받을 수 있음)
    private double bidAmount;  // 입찰 금액
    // 필요시 timestamp, 기타 필드
}
