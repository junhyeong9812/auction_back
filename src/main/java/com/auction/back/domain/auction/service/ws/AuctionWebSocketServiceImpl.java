package com.auction.back.domain.auction.service.ws;

import com.auction.back.domain.auction.dto.ws.BidResultDto;
import com.auction.back.domain.user.entity.User;
import com.auction.back.domain.user.repository.UserRepository;
import com.auction.back.domain.auction.repository.AuctionRepository;
import com.auction.back.domain.auction.entity.Auction;
import com.auction.back.global.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuctionWebSocketServiceImpl implements AuctionWebSocketService {

    private final RedisService redisService;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;

    @Override
    @Transactional
    public BidResultDto placeBid(Long auctionId, double bidAmount, String userEmail) {
        // 1) 사용자 조회 & 포인트 검증
        System.out.println("입찰 서비스 시작");
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));
        if (user.getPointBalance() < bidAmount) {
            throw new RuntimeException("포인트가 부족하여 입찰할 수 없습니다.");
        }

        System.out.println("입찰 서비스 유저검색끝 ");
        // 2) Redis에서 현재 경매 상태, 마감시간, 최고가, 최고입찰자 가져옴
        String statusKey = "auction:" + auctionId + ":status";
        String endTimeKey = "auction:" + auctionId + ":endTime";
        String highestPriceKey = "auction:" + auctionId + ":highestPrice";
        String highestBidderKey = "auction:" + auctionId + ":highestBidder";

        System.out.println("경매진행 확인 ");
        String currentStatus = redisService.getValue(statusKey);
        if (!"ONGOING".equals(currentStatus)) {
            throw new RuntimeException("경매가 진행중이 아니므로 입찰할 수 없습니다. (현재상태=" + currentStatus + ")");
        }
        System.out.println("경매진행 확인 완료 ");
        String endTimeStr = redisService.getValue(endTimeKey);
        // ex) endTimeStr = "2025-01-23T10:30:00" or epoch seconds
        LocalDateTime dynamicEndTime = LocalDateTime.parse(endTimeStr);
        // or parse epoch

        // 마감시간이 지났으면 예외
        if (LocalDateTime.now().isAfter(dynamicEndTime)) {
            throw new RuntimeException("이미 마감된 경매입니다.");
        }
        System.out.println("마감진행 완료 ");

        // 3) 현재 최고가
        String highestPriceStr = redisService.getValue(highestPriceKey);
        double currentHighest = (highestPriceStr == null) ? 0.0 : Double.parseDouble(highestPriceStr);

        if (bidAmount <= currentHighest) {
            throw new RuntimeException("현재 최고가보다 높아야 합니다.");
        }
        System.out.println("최고가 확인 오케이 ");

        // 4) 입찰 성공 => 최고가, 최고입찰자 갱신
        redisService.setValue(highestPriceKey, String.valueOf(bidAmount), 600L);
        redisService.setValue(highestBidderKey, userEmail, 600L);
        System.out.println("갱신 확인  ");
        // ex) 마감 3분 전 입찰 시 5분 연장 = dynamicEndTime.plusMinutes(5)
        //     if(남은시간 <=3분) => dynamicEndTime = now.plusMinutes(5)
        long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), dynamicEndTime);
        // 5) 마감 3분 이하 시 5분 연장
        if (minutesLeft <= 3) {
            LocalDateTime newEndTime = LocalDateTime.now().plusMinutes(5);
            redisService.setValue(endTimeKey, newEndTime.toString(), 600L);

            // dto에 담을 String endTime
            return new BidResultDto(
                    auctionId, userEmail, bidAmount, true, newEndTime.toString()
            );
        }
        System.out.println("함수 종료끝 ");
        // 연장 안 한 경우
        return new BidResultDto(
                auctionId, userEmail, bidAmount, true, null
        );
    }
}

