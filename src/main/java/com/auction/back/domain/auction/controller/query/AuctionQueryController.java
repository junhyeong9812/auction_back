package com.auction.back.domain.auction.controller.query;

import com.auction.back.domain.auction.dto.request.AuctionSearchDto;
import com.auction.back.domain.auction.dto.response.AuctionDetailDto;
import com.auction.back.domain.auction.dto.response.AuctionListDto;
import com.auction.back.domain.auction.entity.Auction;
import com.auction.back.domain.auction.enums.AuctionStatus;
import com.auction.back.domain.auction.service.query.AuctionQueryService;
import com.auction.back.global.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionQueryController {

    private final AuctionQueryService auctionQueryService;
    private final RedisService redisService;

    @GetMapping("/search")
    public Page<AuctionListDto> searchAuctions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            Pageable pageable
    ) {
        // 1) AuctionSearchDto 생성
        AuctionSearchDto searchDto = new AuctionSearchDto();
        if (status != null) {
            searchDto.setStatus(AuctionStatus.valueOf(status));
        }
        searchDto.setKeyword(keyword);

        // 2) Service 호출
        Page<Auction> page = auctionQueryService.searchAuctions(searchDto, pageable);

        // 3) Page<Entity> → Page<DTO> 변환
        return page.map(auction -> {
            AuctionListDto dto = new AuctionListDto();
            dto.setAuctionId(auction.getId());
            dto.setTitle(auction.getTitle());
            dto.setStatus(auction.getStatus());
            dto.setImage(auction.getImage());

            // 상태 분기
            if (auction.getStatus() == AuctionStatus.SCHEDULED) {
                // DB에 저장된 startTime 보여주기
                // price = startPrice
                dto.setPrice(auction.getStartPrice());
                dto.setEndTime(auction.getStartTime().toString()); // 문제에 따라 "시작 시간"을 반환
            }
            else if (auction.getStatus() == AuctionStatus.ONGOING) {
                // Redis endTime, highestPrice
                String prefix = "auction:" + auction.getId() + ":";
                String highestPriceStr = redisService.getValue(prefix + "highestPrice");
                if (highestPriceStr != null) {
                    dto.setPrice(Double.parseDouble(highestPriceStr));
                } else {
                    dto.setPrice(auction.getStartPrice());
                }
                // endTime
                String endTimeStr = redisService.getValue(prefix + "endTime");
                dto.setEndTime(endTimeStr != null ? endTimeStr : "");
            }
            else if (auction.getStatus() == AuctionStatus.ENDED) {
                // DB finalPrice
                Double finalPrice = auction.getFinalPrice();
                dto.setPrice(finalPrice != null ? finalPrice : 0.0);
                dto.setEndTime(auction.getEndTime().toString());
                // 또는 finalEndTime if you store it
            }
            return dto;
        });
    }
    /**
     * 옥션 단건 상세
     * e.g. GET /api/auctions/{auctionId}
     */
    @GetMapping("/{auctionId}")
    public ResponseEntity<AuctionDetailDto> getAuctionDetail(@PathVariable Long auctionId) {
        // 1) Service 호출
        Auction auction = auctionQueryService.findAuctionById(auctionId);

        // 2) Entity -> DTO 변환
        AuctionDetailDto dto = toAuctionDetailDto(auction);

        // 3) 상태가 ONGOING이면 Redis에서 현재가격, endTime 반영
        if (auction.getStatus() == AuctionStatus.ONGOING) {
            String prefix = "auction:" + auction.getId() + ":";
            String highestPriceStr = redisService.getValue(prefix + "highestPrice");
            String endTimeStr = redisService.getValue(prefix + "endTime");
            // 가격
            if (highestPriceStr != null) {
                dto.setPrice(Double.parseDouble(highestPriceStr));
            }
            // 동적 endTime
            if (endTimeStr != null) {
                dto.setEndTime(endTimeStr);
            }
        }

        return ResponseEntity.ok(dto);
    }

    /**
     * Entity -> DTO 변환 메서드
     */
    private AuctionDetailDto toAuctionDetailDto(Auction auction) {
        AuctionDetailDto dto = new AuctionDetailDto();
        dto.setAuctionId(auction.getId());
        dto.setTitle(auction.getTitle());
        dto.setViewCount(auction.getViewCount());
        dto.setStartPrice(auction.getStartPrice());
        dto.setStartTime(auction.getStartTime().toString());
        dto.setEndTime(auction.getEndTime().toString()); // 기본적으로 DB endTime
        dto.setImage(auction.getImage());
        dto.setDescription(auction.getDescription());
        dto.setSpecies(auction.getSpecies());
        dto.setGender(auction.getGender());
        dto.setSize(auction.getSize());
        dto.setSellerLocation(auction.getSellerLocation());
        dto.setStatus(auction.getStatus());
        dto.setFinalPrice(auction.getFinalPrice() != null ? auction.getFinalPrice() : 0.0);
        dto.setFinalEndTime(
                auction.getFinalEndTime() != null
                        ? auction.getFinalEndTime().toString()
                        : null
        );

        // seller.email
        if (auction.getSeller() != null) {
            dto.setSellerEmail(auction.getSeller().getEmail());
        }
        // winner.email
        if (auction.getWinner() != null) {
            dto.setWinnerEmail(auction.getWinner().getEmail());
        }

        // price, endTime 은 상태가 ONGOING일 때 Redis로 덮어쓸 수 있음
        // 여기서는 default 로 DB의 startPrice, endTime
        dto.setPrice(auction.getStartPrice());

        return dto;
    }
}
