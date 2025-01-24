package com.auction.back.domain.auction.repository;

import com.auction.back.domain.auction.dto.request.AuctionSearchDto;
import com.auction.back.domain.auction.entity.Auction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuctionRepositoryCustom {

    /**
     * QueryDSL로 상태 & 키워드 검색 + 페이징
     */
    Page<Auction> searchAuctions(AuctionSearchDto condition, Pageable pageable);
}