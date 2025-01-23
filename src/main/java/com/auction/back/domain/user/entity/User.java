package com.auction.back.domain.user.entity;

import com.auction.back.domain.user.enums.UserRole;
import com.auction.back.domain.user.enums.UserStatus;
import com.auction.back.global.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인에 쓰일 이메일
    @Column(nullable = false, length = 100, unique = true)
    private String email;

    // 비밀번호 (인코딩해서 저장)
    @Column(nullable = false, length = 200)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;          // 이름

    @Column(nullable = false, length = 50, unique = true)
    private String nickname;      // 닉네임

    @Column(length = 20)
    private String phoneNumber;   // 전화번호

    @Column
    private int age;          // 나이

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
    private double pointBalance;  // 포인트 잔액

    public void chargePoint(double amount) {
        this.pointBalance += amount;
    }

    public void usePoint(double amount) {
        this.pointBalance -= amount;
    }
}
