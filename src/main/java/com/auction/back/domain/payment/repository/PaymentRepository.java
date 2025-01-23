package com.auction.back.domain.payment.repository;

import com.auction.back.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // PortOne 결제 고유 ID로 Payment 조회
    Optional<Payment> findByImpUid(String impUid);
}