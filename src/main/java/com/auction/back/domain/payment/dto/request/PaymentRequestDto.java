package com.auction.back.domain.payment.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentRequestDto {
    private String impUid;
    private String email;
}
