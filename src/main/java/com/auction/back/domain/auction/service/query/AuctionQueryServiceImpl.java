package com.auction.back.domain.auction.service.query;

import com.auction.back.domain.auction.dto.request.AuctionSearchDto;
import com.auction.back.domain.auction.entity.Auction;
import com.auction.back.domain.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuctionQueryServiceImpl implements AuctionQueryService {

    private final AuctionRepository auctionRepository;

    @Override
    public Page<Auction> searchAuctions(AuctionSearchDto searchDto, Pageable pageable) {
        return auctionRepository.searchAuctions(searchDto, pageable);
    }
    @Override
    public Auction findAuctionById(Long auctionId) {
        // 조인 fetch or 그냥 getById
        // e.g. using JPA: auctionRepository.findById(auctionId)...
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + auctionId));
    }
}
