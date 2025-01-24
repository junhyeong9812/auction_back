package com.auction.back.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

/** 사용자 이메일 리스트를 감싸는 DTO */
@Data
@AllArgsConstructor
public class UserEmailListDto {
    private List<UserEmailDto> userList;
}