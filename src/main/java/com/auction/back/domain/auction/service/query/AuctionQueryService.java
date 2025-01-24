package com.auction.back.domain.auction.service.query;

import com.auction.back.domain.auction.dto.request.AuctionSearchDto;
import com.auction.back.domain.auction.entity.Auction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuctionQueryService {
    public Page<Auction> searchAuctions(AuctionSearchDto searchDto, Pageable pageable);
    // 단건 조회
    Auction findAuctionById(Long auctionId);
}
