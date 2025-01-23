package com.auction.back.domain.user.service.auth;

import com.auction.back.domain.user.dto.request.LoginRequestDto;
import com.auction.back.domain.user.dto.response.TokenResponse;
import com.auction.back.domain.user.service.query.UserQueryService;
import com.auction.back.global.jwt.TokenProvider;
import com.auction.back.global.redis.RedisService;
import com.auction.back.domain.user.entity.User;
import com.auction.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final UserQueryService userQueryService;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RedisService redisService;

    public TokenResponse login(LoginRequestDto dto) {
        User user = userQueryService.findByEmail(dto.getEmail());
        if (user == null) {
            throw new RuntimeException("존재하지 않는 사용자 이메일");
        }

        // 비밀번호 체크
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호 불일치");
        }

        // Access & Refresh Token 생성
        String accessToken = tokenProvider.createAccessToken(user.getEmail());
        String refreshToken = tokenProvider.createRefreshToken(user.getEmail());

        // Redis에 RefreshToken 저장 (Key: "RT:"+이메일, Value: refreshToken)
        // 만료시간 1일(86400초)
        redisService.setValue("RT:" + user.getEmail(), refreshToken, 86400);

        // 토큰 반환
        return new TokenResponse(accessToken, refreshToken);
    }

    // Refresh Token으로 Access Token 재발행
    public TokenResponse reIssueAccessToken(String refreshToken) {
        // refresh 토큰 검증
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 Refresh Token");
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);

        // Redis에서 "RT:"+email 값 조회
        String savedRefresh = redisService.getValue("RT:" + email);
        if (savedRefresh == null || !savedRefresh.equals(refreshToken)) {
            throw new RuntimeException("만료되거나 존재하지 않는 리프레시 토큰입니다.");
        }

        // 새로운 Access Token 발급
        String newAccessToken = tokenProvider.createAccessToken(email);

        return new TokenResponse(newAccessToken, refreshToken);
        // Refresh는 그대로 재사용
    }
}
