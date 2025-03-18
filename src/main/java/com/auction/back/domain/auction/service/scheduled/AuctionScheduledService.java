package com.auction.back.domain.auction.service.scheduled;

import com.auction.back.domain.auction.entity.Auction;
import com.auction.back.domain.auction.enums.AuctionStatus;
import com.auction.back.domain.auction.repository.AuctionRepository;
import com.auction.back.domain.user.entity.User;
import com.auction.back.domain.user.service.query.UserQueryService;
import com.auction.back.global.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuctionScheduledService {

    private final AuctionRepository auctionRepository;
    private final RedisService redisService;
    private final UserQueryService userQueryService;

    /**
     * 진행중(ONGOING) 경매 중 마감시간 지난 것 처리
     */
    @Transactional
    public void checkAuctionEnd() {
        log.info("checkAuctionEnd 동작");
        List<Auction> ongoingAuctions = auctionRepository.findByStatus(AuctionStatus.ONGOING);
        LocalDateTime now = LocalDateTime.now();

        for (Auction auction : ongoingAuctions) {
            String endTimeStr = redisService.getValue("auction:" + auction.getId() + ":endTime");
            if (endTimeStr == null) continue;

            LocalDateTime dynamicEndTime = LocalDateTime.parse(endTimeStr);
            if (now.isAfter(dynamicEndTime)) {
                doEndAuction(auction);
            }
        }
    }


    /**
     * SCHEDULED 경매 중 시작시간이 지난 것 ONGOING 전환
     */
    @Transactional
    public void checkAuctionStart() {
        log.info("checkAuctionStart 동작");
        List<Auction> scheduledAuctions = auctionRepository.findByStatus(AuctionStatus.SCHEDULED);
        LocalDateTime now = LocalDateTime.now();

        for (Auction auction : scheduledAuctions) {
            if (!now.isBefore(auction.getStartTime())) {
                auction.updateStatus(AuctionStatus.ONGOING);
                auctionRepository.save(auction);

                log.info("경매 시작: auctionId={}", auction.getId());
                initRedisForOngoingAuction(auction);
            }
        }
    }

    private void doEndAuction(Auction auction) {
        long auctionId = auction.getId();
        log.info("경매 마감 처리 시도. auctionId={}", auctionId);

        String prefix = "auction:" + auctionId + ":";
        String highestPriceStr = redisService.getValue(prefix + "highestPrice");
        String highestBidderEmail = redisService.getValue(prefix + "highestBidder");
        String finalEndTimeStr = redisService.getValue(prefix + "endTime");

        double highestPrice = (highestPriceStr == null) ? 0.0 : Double.parseDouble(highestPriceStr);

        // 경매 상태 = ENDED
        auction.updateStatus(AuctionStatus.ENDED);

        if (highestBidderEmail != null && !highestBidderEmail.isEmpty()) {
            User winner = userQueryService.findByEmail(highestBidderEmail);

            if (winner.getPointBalance() < highestPrice) {
                log.warn("낙찰자 포인트 부족. auctionId={}, bidder={}", auctionId, highestBidderEmail);
                // 낙찰 무효 로직 ...
            } else {
                winner.usePoint(highestPrice);
            }

            User seller = auction.getSeller(); // Lazy 필드여도, @Transactional 환경이므로 세션 O
            if (seller != null) {
                seller.chargePoint(highestPrice);
            }

            auction.setWinner(winner, highestPrice);
            log.info("낙찰 완료. winner={}, price={}", winner.getEmail(), highestPrice);
            if (finalEndTimeStr != null) {
                auction.setFinalEndTime(LocalDateTime.parse(finalEndTimeStr));
            }
        } else {
            // 유찰
            auction.setWinner(null, 0.0);
        }

        auctionRepository.save(auction);
        cleanUpRedisKeys(auctionId);
        log.info("경매 마감 완료. auctionId={}", auctionId);
    }

    private void initRedisForOngoingAuction(Auction auction) {
        long auctionId = auction.getId();
        String prefix = "auction:" + auctionId + ":";

        redisService.setValue(prefix + "highestPrice", String.valueOf(auction.getStartPrice()), 3600L);

        LocalDateTime endTime = auction.getEndTime();
        redisService.setValue(prefix + "endTime", endTime.toString(), 3600L);

        redisService.setValue(prefix + "highestBidder", "", 3600L);
        redisService.setValue(prefix + "status", "ONGOING", 3600L);
    }

    private void cleanUpRedisKeys(Long auctionId) {
        String prefix = "auction:" + auctionId + ":";
        redisService.deleteValue(prefix + "highestPrice");
        redisService.deleteValue(prefix + "highestBidder");
        redisService.deleteValue(prefix + "endTime");
        redisService.deleteValue(prefix + "status");
    }
}
