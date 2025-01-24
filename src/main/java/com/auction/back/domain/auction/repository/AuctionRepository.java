package com.auction.back.domain.auction.repository;

import com.auction.back.domain.auction.entity.Auction;
import com.auction.back.domain.auction.enums.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface  AuctionRepository extends JpaRepository<Auction, Long>, AuctionRepositoryCustom {
    List<Auction> findByStatus(AuctionStatus status);
}
