package com.auction.back.domain.auction.controller.ws;

import com.auction.back.domain.auction.dto.ws.BidMessage;
import com.auction.back.domain.auction.dto.ws.BidResultDto;
import com.auction.back.domain.auction.dto.ws.ErrorResultDto;
import com.auction.back.domain.auction.service.ws.AuctionWebSocketService;
import com.auction.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class AuctionStompController {

    private final SimpMessagingTemplate messagingTemplate;
    private final AuctionWebSocketService auctionWebSocketService;

    /**
     * 클라이언트에서
     * stompClient.send("/app/auction/{auctionId}/bid", {...})
     * 로 전송 시 호출됨
     */
    @MessageMapping("/auction/{auctionId}/bid")
    public void handleBid(@DestinationVariable Long auctionId, BidMessage bidMessage) {
        // 1) SecurityContext에서 userEmail
        String userEmail = SecurityUtils.getCurrentUserEmail();
        try {
        // 2) 비즈니스 로직: 입찰 검증 & Redis 업데이트 등
        BidResultDto resultDto = auctionWebSocketService.placeBid(auctionId, bidMessage.getBidAmount(), userEmail);

        // 3) /topic/auction/{auctionId} 로 브로드캐스트
        messagingTemplate.convertAndSend("/topic/auction/" + auctionId, resultDto);
        } catch (RuntimeException e) {
            // 실패 시: 같은 채널("/topic/auction/{auctionId}")에 ErrorResult 전송
            ErrorResultDto errorDto = new ErrorResultDto(
                    false,
                    e.getMessage() // 예: "포인트 부족" 등
            );

            messagingTemplate.convertAndSend(
                    "/topic/auction/" + auctionId,
                    errorDto
            );
        }
        }
}
