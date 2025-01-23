package com.auction.back.domain.auction.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BidResultDto {
    private Long auctionId;
    private String bidderEmail;
    private double highestPrice;
}
