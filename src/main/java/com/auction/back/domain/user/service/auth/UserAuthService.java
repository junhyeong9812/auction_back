package com.auction.back.domain.user.service.auth;

import com.auction.back.domain.user.dto.request.LoginRequestDto;
import com.auction.back.domain.user.dto.response.TokenResponse;

public interface UserAuthService {
    public TokenResponse login(LoginRequestDto dto);
    public TokenResponse reIssueAccessToken(String refreshToken);
}
