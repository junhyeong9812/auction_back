package com.auction.back.domain.payment.service.command;

import com.auction.back.domain.payment.dto.request.PaymentRequestDto;
import com.auction.back.domain.payment.dto.response.PaymentResponseDto;
import com.auction.back.domain.payment.entity.Payment;
import com.auction.back.domain.payment.enums.PaymentStatus;
import com.auction.back.domain.payment.infrastructure.PortOneApiClient;
import com.auction.back.domain.payment.repository.PaymentRepository;
import com.auction.back.domain.user.entity.User;
import com.auction.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentCommandServiceImpl implements PaymentCommandService {

    private final UserQueryService userQueryService;      // 이메일로 User 조회용
    private final PortOneApiClient portOneApiClient;      // PortOne API 호출
    private final PaymentRepository paymentRepository;

    @Override
    public PaymentResponseDto processPayment(PaymentRequestDto requestDto) {
        // 1) 이메일 기반으로 User 조회
        User user = userQueryService.findByEmail(requestDto.getEmail());

        // 2) PortOne 결제정보 조회
        Map<String, Object> paymentData = portOneApiClient.getPaymentInfo(requestDto.getImpUid());

        // paymentData에서 필요한 필드를 추출
        double amount = ((Number) paymentData.get("amount")).doubleValue();
        String statusStr = (String) paymentData.get("status");         // "paid", "failed", "cancelled" etc.
        String pgProvider = (String) paymentData.get("pg_provider");   // ex) html5_inicis, kakao
        String payMethod = (String) paymentData.get("pay_method");     // 카드, 가상계좌 등
        String buyerName = (String) paymentData.get("buyer_name");
        String buyerEmail = (String) paymentData.get("buyer_email");
        String receiptUrl = (String) paymentData.get("receipt_url");

        // paid_at이 UNIX timestamp 형태면 long 변환 후 LocalDateTime으로 변환
        long paidAtUnix = paymentData.get("paid_at") != null
                ? ((Number) paymentData.get("paid_at")).longValue()
                : 0L;
        LocalDateTime paidAt = (paidAtUnix > 0)
                ? LocalDateTime.ofEpochSecond(paidAtUnix, 0, ZoneOffset.UTC)
                : null;

        // PaymentStatus 변환
        PaymentStatus paymentStatus;
        if ("paid".equals(statusStr)) {
            paymentStatus = PaymentStatus.PAID;
        } else if ("cancelled".equals(statusStr)) {
            paymentStatus = PaymentStatus.CANCELED;
        } else {
            paymentStatus = PaymentStatus.FAILED;
        }

        // 3) Payment 엔티티 생성 (Builder 사용)
        Payment payment = Payment.builder()
                .user(user)
                .impUid(requestDto.getImpUid())
                .pgProvider(pgProvider)
                .payMethod(payMethod)
                .paidAmount(amount)
                .status(paymentStatus)
                .paidAt(paidAt)
                .buyerName(buyerName)
                .buyerEmail(buyerEmail)
                .receiptUrl(receiptUrl)
                .build();

        // 4) DB 저장
        Payment saved = paymentRepository.save(payment);

        // 5) 결제 상태가 PAID라면, 포인트 충전
        if (saved.getStatus() == PaymentStatus.PAID) {
            saved.applyPointToUser();  // user.chargePoint(saved.getPaidAmount()) 호출
        }

        // 7) 결과를 PaymentResponseDto로 포장
        boolean isSuccess = (paymentStatus == PaymentStatus.PAID);
        double updatedPoint = user.getPointBalance();  // 결제 후 최신 포인트
        String message = isSuccess ? "결제 및 포인트 충전 성공" : "결제 실패 혹은 취소";

        //8)테스트용 충전 후 환불
        cancelPayment(requestDto.getImpUid());

        return PaymentResponseDto.builder()
                .success(isSuccess)
                .updatedPoint(updatedPoint)
                .message(message)
                .build();
    }

    @Override
    public boolean cancelPayment(String impUid) {
        // 1) PortOne에 실제 취소 요청
        boolean success = portOneApiClient.cancelPayment(impUid, "사용자 요청 환불");
        if (!success) {
            return false; // 환불 실패
        }

        // 2) DB Payment 상태 업데이트
        Payment payment = paymentRepository.findByImpUid(impUid)
                .orElseThrow(() -> new RuntimeException("Payment Not Found - impUid=" + impUid));

        // 엔티티 내부 메서드로 상태 변경
        payment.updateStatus(PaymentStatus.CANCELED);

        // 필요하다면, 포인트 환불 로직도 추가 가능:
        // if (payment.getStatus() == PaymentStatus.PAID) {
        //    payment.getUser().usePoint(payment.getPaidAmount());
        // }

        return true;
    }
}
