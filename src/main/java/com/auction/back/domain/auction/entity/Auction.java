package com.auction.back.domain.auction.entity;

import com.auction.back.domain.auction.enums.AuctionStatus;
import com.auction.back.domain.user.entity.User;
import com.auction.back.global.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
    private String title;

    @Column(nullable = false)
    private int viewCount;

    @Column(nullable = false)
    private int startPrice;

    @Column(nullable = false)
    private LocalDateTime startTime;  // 경매 시작시간 (DB 기록용)

    @Column(nullable = false)
    private LocalDateTime endTime;    // 경매 마감시간 (DB 기록용) - 실제 진행중에는 Redis에서 변경

    @Column(length = 255)
    private String image;

    @Lob
    private String description;

    @Column(length = 50)
    private String species;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 50)
    private String size;

    @Column(length = 100)
    private String sellerLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuctionStatus status; // SCHEDULED, ONGOING, ENDED, CANCELED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    // 낙찰자 (경매 종료 후 세팅)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;   // 최종 입찰자

    // 낙찰 금액
    private Double finalPrice;

    @Column
    private LocalDateTime finalEndTime;

    //== 편의 메서드 ==//
    public void updateImage(String filename) {
        this.image = filename;
    }

    public void updateStatus(AuctionStatus newStatus) {
        this.status = newStatus;
    }

    public void setWinner(User winner, double price) {
        this.winner = winner;
        this.finalPrice = price;
    }
    public void setFinalEndTime(LocalDateTime finalEndTime) {
        this.endTime = finalEndTime; // DB의 endTime을 덮어씀
    }

    /**
     * 경매 정보 수정 (SCHEDULED 상태일 때만 가능, startTime 30분 전까지 등)
     */
    public void updateAuction(String newTitle, int newStartPrice, String newSpecies, String newDescription) {
        // 예: startTime 30분 전이면 수정 불가
        long minutesUntilStart = ChronoUnit.MINUTES.between(LocalDateTime.now(), this.startTime);
        if (minutesUntilStart <= 30) {
            throw new RuntimeException("경매 시작 30분 전 이후로는 수정 불가.");
        }
        if (this.status != AuctionStatus.SCHEDULED) {
            throw new RuntimeException("진행중/종료된 경매는 수정 불가.");
        }

        this.title = newTitle;
        this.startPrice = newStartPrice;
        this.species = newSpecies;
        this.description = newDescription;
    }
}
