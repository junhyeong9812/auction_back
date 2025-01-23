package com.auction.back.domain.auction.service.ws;

import com.auction.back.domain.auction.dto.ws.BidResultDto;

public interface AuctionWebSocketService {
    public BidResultDto placeBid(Long auctionId, double bidAmount, String userEmail);
}
