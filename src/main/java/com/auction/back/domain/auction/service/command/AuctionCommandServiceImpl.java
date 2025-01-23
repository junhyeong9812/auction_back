package com.auction.back.domain.auction.service.command;

import com.auction.back.domain.auction.dto.request.AuctionCreateRequestDto;
import com.auction.back.domain.auction.dto.request.AuctionUpdateRequestDto;
import com.auction.back.domain.auction.entity.Auction;
import com.auction.back.domain.auction.enums.AuctionStatus;
import com.auction.back.domain.auction.repository.AuctionRepository;
import com.auction.back.domain.user.entity.User;
import com.auction.back.domain.user.service.query.UserQueryService;
import com.auction.back.global.enums.Gender;
import com.auction.back.global.utils.FileStorageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Transactional
public class AuctionCommandServiceImpl implements AuctionCommandService {

    private final AuctionRepository auctionRepository;
    private final UserQueryService userQueryService;

    // 이미지 저장 폴더: user.dir + "/src/main/resources/images/auction"
    private static final String BASE_DIRECTORY = System.getProperty("user.dir") + "/src/main/resources/images/auction";

    @Override
    public Long createAuction(AuctionCreateRequestDto dto, String userEmail) {
        // 1) 사용자 조회
        User seller = userQueryService.findByEmail(userEmail);

        // 2) Auction 엔티티 생성
        Auction auction = Auction.builder()
                .title(dto.getTitle())
                .viewCount(0)
                .startPrice(dto.getStartPrice())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .image(null)
                .description(dto.getDescription())
                .species(dto.getSpecies())
                .gender(dto.getGender() != null ? Gender.valueOf(dto.getGender()) : null)
                .size(dto.getSize())
                .sellerLocation(dto.getSellerLocation())
                .status(AuctionStatus.SCHEDULED)
                .seller(seller)
                .build();

        // DB 저장
        Auction savedAuction = auctionRepository.save(auction);

        // 3) 이미지 파일 처리
        MultipartFile imageFile = dto.getImageFile();
        if (imageFile != null && !imageFile.isEmpty()) {
            String extension = getExtension(imageFile.getOriginalFilename());
            String filename = "Auction_" + savedAuction.getId() + (extension != null ? extension : ".jpg");

            try {
                FileStorageUtils.storeFile(BASE_DIRECTORY, filename, imageFile);
                savedAuction.updateImage(filename);
            } catch (IOException e) {
                e.printStackTrace();
                // or throw custom exception
            }
        }

        return savedAuction.getId();
    }

    @Override
    public void updateAuction(Long auctionId,
                              AuctionUpdateRequestDto dto,
                              String userEmail) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found: id=" + auctionId));

        // 권한 체크
        if (!auction.getSeller().getEmail().equals(userEmail)) {
            throw new RuntimeException("경매 수정 권한이 없습니다.");
        }

        // 경매 엔티티에 대한 도메인 로직(제목/가격/종/설명) 업데이트
        auction.updateAuction(
                dto.getTitle(),
                dto.getStartPrice(),
                dto.getSpecies(),
                dto.getDescription()
        );

        // 새 이미지 파일이 있다면
        MultipartFile newImageFile = dto.getImageFile();
        if (newImageFile != null && !newImageFile.isEmpty()) {
            // 기존 이미지 삭제
            if (auction.getImage() != null) {
                FileStorageUtils.deleteFile(BASE_DIRECTORY, auction.getImage());
            }

            // 새 이미지 저장
            String extension = getExtension(newImageFile.getOriginalFilename());
            String newFilename = "Auction_" + auction.getId() + (extension != null ? extension : ".jpg");

            try {
                FileStorageUtils.storeFile(BASE_DIRECTORY, newFilename, newImageFile);
                auction.updateImage(newFilename);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void cancelAuction(Long auctionId, String userEmail) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found: id=" + auctionId));

        if (!auction.getSeller().getEmail().equals(userEmail)) {
            throw new RuntimeException("경매 취소 권한이 없습니다.");
        }
        auction.updateStatus(AuctionStatus.CANCELED);
    }

    private String getExtension(String filename) {
        if (filename == null) return null;
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1) {
            return null;
        }
        return filename.substring(dotIndex);
    }
}
