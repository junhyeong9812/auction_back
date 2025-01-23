package com.auction.back.domain.payment.service.command;

import com.auction.back.domain.payment.dto.request.PaymentRequestDto;
import com.auction.back.domain.payment.dto.response.PaymentResponseDto;

public interface PaymentCommandService {
    public PaymentResponseDto processPayment(PaymentRequestDto requestDto);
    public boolean cancelPayment(String impUid);
}
