package com.auction.back.domain.payment.dto.response;

import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDto {
    private boolean success;          // 결제 처리 성공 여부
    private double updatedPoint;      // 갱신된 유저 포인트 잔액
    private String message;           // "결제 성공" or 에러 메세지
}

