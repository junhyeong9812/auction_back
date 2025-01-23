package com.auction.back.domain.user.entity;

import com.auction.back.domain.user.enums.UserRole;
import com.auction.back.domain.user.enums.UserStatus;
import com.auction.back.global.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users") // 테이블명은 예시
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;              // PK

    @Column(nullable = false, length = 50)
    private String name;          // 이름

    @Column(nullable = false, length = 50, unique = true)
    private String nickname;      // 닉네임

    @Column(nullable = false, length = 100, unique = true)
    private String email;         // 이메일

    @Column(length = 20)
    private String phoneNumber;   // 전화번호

    @Column(length = 255)
    private String introduction;  // 한줄소개

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;        // MALE, FEMALE, NONE

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserStatus status;    // ACTIVE, INACTIVE, DELETED

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserRole role;        // ADMIN, USER

    @Column(nullable = false)
    private double pointBalance;    //사용자 포인트 잔액

    /**
     * 포인트 충전 메서드
     * - 결제 완료시 chargePoint(amount)를 호출
     */
    public void chargePoint(double amount) {
        this.pointBalance += amount;
    }

    /**
     * 포인트 사용 메서드
     * - 경매 입찰/결제 등에서 사용
     */
    public void usePoint(double amount) {
        // 잔액 체크 로직 필요 (this.pointBalance >= amount 등)
        this.pointBalance -= amount;
    }
}
