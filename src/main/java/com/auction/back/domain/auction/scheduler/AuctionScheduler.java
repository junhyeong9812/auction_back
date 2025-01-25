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
//    @Scheduled(fixedRate = 1000)
    @Scheduled(fixedDelay = 1000)
    public void checkAuctionEnd() {
        System.out.println("checkAuctionEnd동작 ");
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
     * 1초마다 SCHEDULED 상태 경매 중
     * 시작시간이 지났으면 ONGOING으로 변경 & Redis에 시작가/마감시간 저장
     */
//    @Scheduled(fixedRate = 1000)
    @Scheduled(fixedDelay = 1000)
    public void checkAuctionStart() {
        System.out.println("checkAuctionStart동작 ");
        List<Auction> scheduledAuctions = auctionRepository.findByStatus(AuctionStatus.SCHEDULED);
        LocalDateTime now = LocalDateTime.now();

        for (Auction auction : scheduledAuctions) {
            if (!now.isBefore(auction.getStartTime())) {
                // 경매 시작
                auction.updateStatus(AuctionStatus.ONGOING);
                auctionRepository.save(auction);

                log.info("경매 시작: auctionId={}", auction.getId());

                // Redis에 초기 최고가(= startPrice), 마감시간(= now+30분) 저장
                initRedisForOngoingAuction(auction);
            }
        }
    }

    /**
     * 경매를 마감 처리
     */
    private void doEndAuction(Auction auction) {
        long auctionId = auction.getId();
        log.info("경매 마감 처리 시도. auctionId={}", auctionId);

        // 최고가 & 최고입찰자
        String prefix = "auction:" + auctionId + ":";
        String highestPriceStr = redisService.getValue(prefix + "highestPrice");
        String highestBidderEmail = redisService.getValue(prefix + "highestBidder");
        String FinalEndTime=redisService.getValue(prefix + "endTime");
        double highestPrice = (highestPriceStr == null) ? 0.0 : Double.parseDouble(highestPriceStr);

        // DB 경매 상태 = ENDED
        auction.updateStatus(AuctionStatus.ENDED);

        if (highestBidderEmail != null) {
            User winner = userQueryService.findByEmail(highestBidderEmail);

            // 낙찰자 포인트 차감(이미 차감 정책이라면 skip)
            if (winner.getPointBalance() < highestPrice) {
                log.warn("낙찰자 포인트 부족. auctionId={}, bidder={}", auctionId, highestBidderEmail);
                // 예: 낙찰 무효 or 다음 최고입찰자 로직 등
            } else {
                winner.usePoint(highestPrice);
            }

            // 판매자 포인트 증가
            User seller = auction.getSeller();
            if (seller != null) {
                seller.chargePoint(highestPrice);
            }

            auction.setWinner(winner, highestPrice);
            log.info("낙찰 완료. winner={}, price={}", winner.getEmail(), highestPrice);
            auction.setFinalEndTime(LocalDateTime.parse(FinalEndTime));
        } else {
            // 유찰
            auction.setWinner(null, 0.0);
        }

        auctionRepository.save(auction);
        cleanUpRedisKeys(auctionId);
        log.info("경매 마감 완료. auctionId={}", auctionId);
    }

    /**
     * 경매를 ONGOING으로 전환할 때,
     * Redis에 초기 최고가/마감시간(30분 뒤) 등을 세팅
     */
    private void initRedisForOngoingAuction(Auction auction) {
        long auctionId = auction.getId();
        String prefix = "auction:" + auctionId + ":";

        // 최고가 = startPrice
        redisService.setValue(prefix + "highestPrice",
                String.valueOf(auction.getStartPrice()),
                3600L);

        // 현재시각 + 30분
//        LocalDateTime endTime = LocalDateTime.now().plusMinutes(30);
        LocalDateTime endTime = auction.getEndTime();
        redisService.setValue(prefix + "endTime", endTime.toString(), 3600L);

        // 최고입찰자, 상태 등 필요하면 추가
        redisService.setValue(prefix + "highestBidder", "", 3600L);
        redisService.setValue(prefix + "status", "ONGOING", 3600L);
    }

    /**
     * 경매 종료 후 Redis 키 정리
     */
    private void cleanUpRedisKeys(Long auctionId) {
        String prefix = "auction:" + auctionId + ":";
        redisService.deleteValue(prefix + "highestPrice");
        redisService.deleteValue(prefix + "highestBidder");
        redisService.deleteValue(prefix + "endTime");
        redisService.deleteValue(prefix + "status");
    }
}
