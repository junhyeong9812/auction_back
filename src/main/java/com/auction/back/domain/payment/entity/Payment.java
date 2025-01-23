package com.auction.back.domain.payment.entity;

import com.auction.back.domain.payment.enums.PaymentStatus;
import com.auction.back.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 유저가 결제했는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // PortOne 결제 unique ID (imp_uid)
    @Column(nullable = false)
    private String impUid;

    // PG사 이름 (예: kakao, html5_inicis 등)
    private String pgProvider;

    // 결제 수단 (카드, 가상계좌, etc.)
    private String payMethod;

    // 결제 금액 (PortOne API에서 검증한 실제 금액)
    @Column(nullable = false)
    private double paidAmount;

    // 결제 상태 (PAID, CANCELED, FAILED 등)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    private LocalDateTime paidAt;       // 결제 시각
    private String buyerName;           // 구매자 이름
    private String buyerEmail;          // 구매자 이메일
    private String receiptUrl;          // 영수증 URL

    // 결제 완료 후 User에게 포인트 반영 (포인트 충전용 결제라고 가정)
    public void applyPointToUser() {
        if (this.status == PaymentStatus.PAID && user != null) {
            user.chargePoint(this.paidAmount);
        }
    }
}
