package com.auction.back.domain.auction.entity;

import com.auction.back.domain.auction.enums.AuctionStatus;
import com.auction.back.global.enums.Gender;
import com.auction.back.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auction")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;              // PK

    @Column(nullable = false, length = 100)
    private String title;         // 경매 제목

    @Column(nullable = false)
    private int viewCount;        // 조회수

    @Column(nullable = false)
    private int startPrice;       // 시작가격

    @Column(nullable = false)
    private LocalDateTime startTime;  // 시작시간

    @Column(nullable = false)
    private LocalDateTime endTime;    // 마감시간

    @Column(length = 255)
    private String image;         // 이미지 경로/URL

    @Lob
    private String description;   // 상세설명

    @Column(length = 50)
    private String species;       // 종

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 10)
    private Gender gender;        // 경매 물품의 성별

    @Column(length = 50)
    private String size;          // 크기

    @Column(length = 100)
    private String sellerLocation;// 판매자 지역

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuctionStatus status; // SCHEDULED, ONGOING, ENDED, CANCELED

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;          // 판매자와 1:1 관계 (Cascade 미적용)
}
