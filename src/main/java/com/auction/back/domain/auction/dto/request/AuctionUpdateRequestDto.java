package com.auction.back.domain.auction.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AuctionUpdateRequestDto {
    private String title;
    private int startPrice;
    private String species;
    private String description;
    // 새 이미지를 업로드할 경우 (optional)
    private MultipartFile imageFile;
}
