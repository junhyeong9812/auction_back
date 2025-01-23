package com.auction.back.domain.auction.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class AuctionCreateRequestDto {

    private String title;
    private int startPrice;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    private String species;
    private String gender;       // "MALE", "FEMALE", "NONE"
    private String size;
    private String sellerLocation;
    private String description;

    // 이미지(멀티파트 파일)
    private MultipartFile imageFile;
}
