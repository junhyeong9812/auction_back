package com.auction.back.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

/** 단일 사용자 이메일 DTO */
@Data
@AllArgsConstructor
public class UserEmailDto {
    private String email;
}