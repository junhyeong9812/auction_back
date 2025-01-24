package com.auction.back.domain.auction.dto.request;

import com.auction.back.domain.auction.enums.AuctionStatus;
import lombok.Data;
import org.springframework.data.domain.Pageable;

@Data
public class AuctionSearchDto {
    private AuctionStatus status; // SCHEDULED, ONGOING, ENDED, ...
    private String keyword;       // 제목/설명 등에서 LIKE 검색
}
