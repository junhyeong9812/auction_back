package com.auction.back.global.config;

import com.auction.back.domain.auction.entity.Auction;
import com.auction.back.domain.auction.enums.AuctionStatus;
import com.auction.back.domain.auction.repository.AuctionRepository;
import com.auction.back.domain.user.entity.User;
import com.auction.back.domain.user.enums.UserRole;
import com.auction.back.domain.user.enums.UserStatus;
import com.auction.back.domain.user.repository.UserRepository;
import com.auction.back.global.enums.Gender;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 이미 데이터가 존재하면 초기화 스킵 (예시)
        if (userRepository.count() > 0 || auctionRepository.count() > 0) {
            return;
        }

        // 1) 유저 10명 생성 (관리자1 + 일반9)
        List<User> users = createUsers();
        userRepository.saveAll(users);

        // 찾기 (관리자, 일반유저1)
        User admin = users.stream()
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .findFirst().orElse(null);

        User user1 = users.stream()
                .filter(u -> u.getRole() == UserRole.USER)
                .findFirst().orElse(null);

        // 2) 옥션 60개 생성: SCHEDULED(20), ONGOING(20), ENDED(20)
        List<Auction> auctions = new ArrayList<>();
        // 현재 시각
        LocalDateTime now = LocalDateTime.now();

        // 2-1) SCHEDULED 20개
        //  - 시작시간: now + 30분
        //  - 마감시간: 시작시간 + 30분
        for (int i = 1; i <= 20; i++) {
            LocalDateTime startTime = now.plusMinutes(30);
            LocalDateTime endTime = startTime.plusMinutes(30);

            Auction auction = Auction.builder()
                    .title("Scheduled Auction #" + i)
                    .viewCount(0)
                    .startPrice(10000 + i * 100)
                    .startTime(startTime)
                    .endTime(endTime)
                    .image("Auction_dummy.jpg") // 더미
                    .description("SCHEDULED 경매 테스트 " + i)
                    .species("기타")
                    .gender(Gender.NONE)
                    .size("N/A")
                    .sellerLocation("테스트 지역")
                    .status(AuctionStatus.SCHEDULED)
                    .seller(admin)  // 모든 옥션의 판매자를 admin으로
                    .build();
            auctions.add(auction);
        }

        // 2-2) ONGOING 20개
        //  - 시작시간: now.minusMinutes(5) (이미 시작됨)
        //  - 마감시간: now.plusMinutes(25) (30분 후 종료)
        for (int i = 1; i <= 20; i++) {
            LocalDateTime startTime = now.minusMinutes(5);
            LocalDateTime endTime = now.plusMinutes(25);

            Auction auction = Auction.builder()
                    .title("Ongoing Auction #" + i)
                    .viewCount(0)
                    .startPrice(20000 + i * 100)
                    .startTime(startTime)
                    .endTime(endTime)
                    .image("Auction_dummy.jpg")
                    .description("ONGOING 경매 테스트 " + i)
                    .species("기타")
                    .gender(Gender.NONE)
                    .size("M")
                    .sellerLocation("테스트 지역")
                    .status(AuctionStatus.SCHEDULED)
                    .seller(admin)
                    .build();
            auctions.add(auction);
        }

        // 2-3) ENDED 20개
        //  - 시작시간: now.minusHours(1)
        //  - 마감시간: now.minusMinutes(30) (이미 종료)
        //  - 낙찰자: 예시로 user1에게 낙찰, finalPrice = startPrice
        //  - point 조정 (user1.usePoint(...), admin.chargePoint(...))
        //    => 여기서는 그냥 DB에만 저장하고, 포인트 수정은 예시
        for (int i = 1; i <= 20; i++) {
            LocalDateTime startTime = now.minusHours(1);
            LocalDateTime endTime = now.minusMinutes(30);

            Auction auction = Auction.builder()
                    .title("Ended Auction #" + i)
                    .viewCount(0)
                    .startPrice(30000 + i * 100)
                    .startTime(startTime)
                    .endTime(endTime)
                    .image("Auction_dummy.jpg")
                    .description("ENDED 경매 테스트 " + i)
                    .species("기타")
                    .gender(Gender.MALE)
                    .size("L")
                    .sellerLocation("테스트 지역")
                    .status(AuctionStatus.ENDED)
                    .seller(admin)
                    .winner(user1) // 낙찰자
                    .finalPrice((double)(30000 + i * 100)) // 낙찰가 = 시작가
                    .finalEndTime(endTime) // 실제 종료 시각
                    .build();
            auctions.add(auction);

            // 포인트 조정 예시:
            // user1 -> 포인트 차감
            // admin -> 포인트 증가
            // (DB flush 시점에 반영)
            user1.usePoint((double)(30000 + i * 100));
            admin.chargePoint((double)(30000 + i * 100));
        }

        auctionRepository.saveAll(auctions);

        // user1, admin 포인트 변경사항 반영
        userRepository.save(user1);
        userRepository.save(admin);

        System.out.println("=== Test Data Initialization Complete ===");
    }

    /**
     * 관리자 1, 일반유저 9
     * 모든 유저 pointBalance = 100000
     */
    private List<User> createUsers() {
        List<User> list = new ArrayList<>();

        // 1) 관리자
        User admin = User.builder()
                .email("admin@exam.com")
                .password(passwordEncoder.encode("admin1234"))
                .name("관리자")
                .nickname("AdminUser")
                .phoneNumber("010-1111-2222")
                .age(30)
                .gender(Gender.NONE)
                .status(UserStatus.ACTIVE)
                .role(UserRole.ADMIN)
                .pointBalance(100000)
                .build();
        list.add(admin);

        // 2) 일반 사용자 9명
        for (int i = 1; i <= 9; i++) {
            User user = User.builder()
                    .email("user" + i + "@exam.com")
                    .password(passwordEncoder.encode("user" + i + "pw"))
                    .name("사용자" + i)
                    .nickname("UserNick" + i)
                    .phoneNumber("010-1234-000" + i)
                    .age(20 + i)
                    .gender((i % 2 == 0) ? Gender.MALE : Gender.FEMALE)
                    .status(UserStatus.ACTIVE)
                    .role(UserRole.USER)
                    .pointBalance(100000)
                    .build();
            list.add(user);
        }

        return list;
    }
}
