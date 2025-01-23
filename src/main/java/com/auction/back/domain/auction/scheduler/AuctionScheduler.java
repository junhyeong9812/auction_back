package com.auction.back.domain.auction.scheduler;

import com.auction.back.domain.auction.entity.Auction;
import com.auction.back.domain.auction.enums.AuctionStatus;
import com.auction.back.domain.auction.repository.AuctionRepository;
import com.auction.back.domain.user.entity.User;
import com.auction.back.domain.user.service.query.UserQueryService;
import com.auction.back.global.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final RedisService redisService;
    private final UserQueryService userQueryService;

    /**
     * 1초마다 진행중(ONGOING)인 경매 중에서, endTime 지난 것들을 검색 → 마감 처리
     */
    @Scheduled(fixedRate = 1000)
    public void checkAuctionEnd() {
        // 1) ONGOING 상태의 경매를 DB에서 조회
        List<Auction> ongoingAuctions = auctionRepository.findByStatus(AuctionStatus.ONGOING);

        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : ongoingAuctions) {
            // Redis endTime
            String endTimeStr = redisService.getValue("auction:" + auction.getId() + ":endTime");
            if (endTimeStr == null) {
                // if not set, fallback to DB의 endTime or skip
                continue;
            }
            LocalDateTime dynamicEndTime = LocalDateTime.parse(endTimeStr);
            if (now.isAfter(dynamicEndTime)) {
                // 마감 처리
                doEndAuction(auction);
            }
        }
    }

    /**
     * 1초마다 스케줄링으로 scheduled(예정상태)인 경매 중 startTime 지난 것들을 ONGOING으로 바꾸는 로직도 가능
     */
    @Scheduled(fixedRate = 1000)
    public void checkAuctionStart() {
        List<Auction> scheduledAuctions = auctionRepository.findByStatus(AuctionStatus.SCHEDULED);
        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : scheduledAuctions) {
            if (!now.isBefore(auction.getStartTime())) {
                // 경매 시작
                auction.updateStatus(AuctionStatus.ONGOING);
                auctionRepository.save(auction);

                log.info("경매 시작: auctionId={}", auction.getId());
            }
        }
    }

    private void doEndAuction(Auction auction) {
        long auctionId = auction.getId();

        String highestPriceStr = redisService.getValue("auction:" + auctionId + ":highestPrice");
        String highestBidderEmail = redisService.getValue("auction:" + auctionId + ":highestBidder");
        double highestPrice = (highestPriceStr == null) ? 0.0 : Double.parseDouble(highestPriceStr);

        // 경매 종료 처리
        auction.updateStatus(AuctionStatus.ENDED);

        // 낙찰자
        if (highestBidderEmail != null) {
            // 낙찰자
            User winner = userQueryService.findByEmail(highestBidderEmail);

            // 낙찰자 포인트 차감(혹은 이미 차감했을 수도 있음)
            // 여기서는 "최종 차감" 방식이라고 가정
            if (winner.getPointBalance() < highestPrice) {
                // 포인트 부족 -> 실무에선 낙찰 무효 or 예외 처리
                log.warn("낙찰자 포인트 부족. auctionId={}, bidder={}", auctionId, highestBidderEmail);
            } else {
                winner.usePoint(highestPrice);
                log.info("낙찰자 포인트 차감. user={}, amount={}", winner.getEmail(), highestPrice);
            }

            // 판매자 포인트 증가
            User seller = auction.getSeller();
            if (seller != null) {
                seller.chargePoint(highestPrice);
                log.info("판매자 포인트 증가. seller={}, amount={}", seller.getEmail(), highestPrice);
            }

            // 엔티티에 낙찰자/금액 기록
            auction.setWinner(winner, highestPrice);
        } else {
            // 낙찰자가 없음 → 유찰
            auction.setWinner(null, 0.0);
        }

        // DB save
        auctionRepository.save(auction);

        // Redis key 정리
        // redisService.deleteValue("auction:" + auctionId + ":endTime");
        // redisService.deleteValue("auction:" + auctionId + ":highestPrice");
        // redisService.deleteValue("auction:" + auctionId + ":highestBidder");
        log.info("경매 마감 완료. auctionId={}", auctionId);
    }

}
