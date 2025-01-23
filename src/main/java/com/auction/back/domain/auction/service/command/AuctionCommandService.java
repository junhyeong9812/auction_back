package com.auction.back.domain.auction.service.command;

import com.auction.back.domain.auction.dto.request.AuctionCreateRequestDto;
import com.auction.back.domain.auction.dto.request.AuctionUpdateRequestDto;

public interface AuctionCommandService {
    Long createAuction(AuctionCreateRequestDto dto, String userEmail);
    void cancelAuction(Long auctionId, String userEmail);
    void updateAuction(Long auctionId, AuctionUpdateRequestDto dto, String userEmail);
}

