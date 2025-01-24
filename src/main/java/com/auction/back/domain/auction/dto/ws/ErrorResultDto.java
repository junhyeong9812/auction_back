package com.auction.back.domain.auction.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResultDto {
    private boolean success; // false
    private String errorMessage;
}
