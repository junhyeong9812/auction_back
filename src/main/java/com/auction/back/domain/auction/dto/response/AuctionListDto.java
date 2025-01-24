package com.auction.back.domain.auction.dto.response;

import com.auction.back.domain.auction.enums.AuctionStatus;
import lombok.Data;

@Data
public class AuctionListDto {
    private Long auctionId;
    private String title;
    private AuctionStatus status;
    private String image;

    private double price;       // 현재 표시할 가격 (ONGOING은 Redis 최고가, SCHEDULED은 startPrice, ENDED은 finalPrice 등)
    private String startTime;
    private String endTime;     // 표시할 마감 시간 (ONGOING은 Redis 동적endTime, SCHEDULED/ENDED은 DB endTime)
}
