package com.auction.back.domain.auction.dto.response;

import com.auction.back.domain.auction.enums.AuctionStatus;
import com.auction.back.global.enums.Gender;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuctionDetailDto {

    private Long auctionId;
    private String title;
    private int viewCount;
    private int startPrice;
    private String startTime; // String 으로 변환
    private String endTime;   // DB endTime
    private String image;
    private String description;
    private String species;
    private Gender gender;
    private String size;
    private String sellerLocation;
    private AuctionStatus status;

    private double finalPrice;      // 낙찰 금액
    private String finalEndTime;    // 실제 최종 종료시간 (optional)

    private String sellerEmail;     // 판매자 이메일
    private String winnerEmail;     // 낙찰자 이메일

    // price: 현재 표시할 가격 (ON-GOING은 Redis highestPrice)
    private double price;
}

