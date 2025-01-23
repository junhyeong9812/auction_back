package com.auction.back.domain.auction.controller.command;

import com.auction.back.domain.auction.dto.request.AuctionCreateRequestDto;
import com.auction.back.domain.auction.dto.request.AuctionUpdateRequestDto;
import com.auction.back.domain.auction.service.command.AuctionCommandService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auction")
@RequiredArgsConstructor
public class AuctionCommandController {

    private final AuctionCommandService auctionCommandService;

    /**
     * 경매 생성
     */
    @PostMapping
    public ResponseEntity<?> createAuction(@ModelAttribute AuctionCreateRequestDto dto,
                                           HttpServletRequest request) {
        try {
            // 실제론 SecurityContext에서 사용자 email
            String userEmail = "testUser@example.com";
            Long auctionId = auctionCommandService.createAuction(dto, userEmail);
            return ResponseEntity.ok("Auction created with ID=" + auctionId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 경매 취소
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelAuction(@PathVariable Long id,
                                           HttpServletRequest request) {
        try {
            String userEmail = "testUser@example.com";
            auctionCommandService.cancelAuction(id, userEmail);
            return ResponseEntity.ok("경매가 취소되었습니다. ID=" + id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    /**
     * 경매 업데이트
     */
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateAuction(@PathVariable Long id,
                                           @ModelAttribute AuctionUpdateRequestDto dto, // <-- 변경
                                           HttpServletRequest request) {
        try {
            // 실제로는 SecurityContextHolder 등에서 userEmail
            String userEmail = "testUser@example.com";

            auctionCommandService.updateAuction(id, dto, userEmail);
            return ResponseEntity.ok("경매가 업데이트되었습니다. ID=" + id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

