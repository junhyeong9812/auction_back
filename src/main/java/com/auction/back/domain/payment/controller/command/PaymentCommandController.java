package com.auction.back.domain.payment.controller.command;

import com.auction.back.domain.payment.dto.request.PaymentRequestDto;
import com.auction.back.domain.payment.dto.response.PaymentResponseDto;
import com.auction.back.domain.payment.service.command.PaymentCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentCommandController {

    private final PaymentCommandService paymentCommandService;

    /**
     * 결제 완료 핸들링
     * - 프론트에서 impUid, email을 JSON으로 받는다 (PaymentRequestDto)
     * - 서비스에서 PortOne 결제검증 + DB 저장 + 포인트 충전
     */
    @PostMapping("/complete")
    public ResponseEntity<?> completePayment(@RequestBody PaymentRequestDto requestDto) {
        try {
            // 만약 성공/실패만 프론트에 주고 싶다면,
            // PaymentDto 대신 Boolean, or custom DTO를 반환해도 된다.
            PaymentResponseDto result = paymentCommandService.processPayment(requestDto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


}
