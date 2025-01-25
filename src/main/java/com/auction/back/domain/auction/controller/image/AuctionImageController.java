package com.auction.back.domain.auction.controller.image;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/auction")
@RequiredArgsConstructor
public class AuctionImageController {

    private static final String BASE_DIRECTORY = System.getProperty("user.dir") + "/src/main/resources/images/auction";

    @GetMapping("/image/{filename}")
    public ResponseEntity<byte[]> getAuctionImage(@PathVariable("filename") String filename) {
        try {
            Path imagePath = Paths.get(BASE_DIRECTORY, filename);
            if (!Files.exists(imagePath)) {
                return ResponseEntity.notFound().build();
            }
            byte[] imageBytes = Files.readAllBytes(imagePath);
            // MIME TYPE 예: image/jpeg
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            // 필요 시 이미지 확장자 파악
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

