package com.auction.back.domain.user.controller.auth;

import com.auction.back.domain.user.dto.request.EmailLoginRequestDto;
import com.auction.back.domain.user.dto.request.LoginRequestDto;
import com.auction.back.domain.user.dto.response.TokenResponse;
import com.auction.back.domain.user.service.auth.UserAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto dto, HttpServletResponse response) {
        try {
            TokenResponse tokenResponse = userAuthService.login(dto);
            // Access Token 쿠키
            long accessTokenExpireSeconds = 30 * 60; // 예) 30분
            ResponseCookie accessCookie = createCookie("ACCESS_TOKEN",
                    tokenResponse.getAccessToken(),
                    accessTokenExpireSeconds,
                    true);

            // Refresh Token 쿠키
            long refreshTokenExpireSeconds = 24 * 60 * 60; // 예) 1일
            ResponseCookie refreshCookie = createCookie("REFRESH_TOKEN",
                    tokenResponse.getRefreshToken(),
                    refreshTokenExpireSeconds,
                    true);

            // 응답 헤더에 Set-Cookie 추가
            response.addHeader("Set-Cookie", accessCookie.toString());
            response.addHeader("Set-Cookie", refreshCookie.toString());

            // 바디에는 굳이 토큰을 실어줄 필요가 없을 수 있음(쿠키를 쓸거면)
            return ResponseEntity.ok("로그인 성공(쿠키로 토큰 전달).");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Refresh Token으로 새 Access Token 발급
     * 예: POST /api/auth/refresh?refreshToken=xxx
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1) 쿠키에서 refreshToken 찾기
            String refreshToken = null;
            if (request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    if ("REFRESH_TOKEN".equals(c.getName())) {
                        refreshToken = c.getValue();
                    }
                }
            }
            if (refreshToken == null) {
                return ResponseEntity.badRequest().body("Refresh token not found in cookies");
            }

            // 2) reIssueAccessToken
            TokenResponse tokenResponse = userAuthService.reIssueAccessToken(refreshToken);

            // 3) 새 Access Token을 쿠키로 내려줌
            ResponseCookie newAccessCookie = createCookie("ACCESS_TOKEN",
                    tokenResponse.getAccessToken(),
                    1800L, // 30분
                    true);
            response.addHeader("Set-Cookie", newAccessCookie.toString());

            return ResponseEntity.ok("새 Access Token 발급됨 (쿠키로 전달)");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 이메일만으로 로그인 (테스트용)
     */
    @PostMapping("/emailLogin")
    public ResponseEntity<?> emailLogin(@RequestBody EmailLoginRequestDto dto,
                                        HttpServletResponse response) {
        try {
            // 1) 이메일만으로 로그인
            TokenResponse tokenResponse = userAuthService.emailOnlyLogin(dto.getEmail());

            // 2) 쿠키 생성
            long accessTokenExpireSeconds = 30 * 60; // 30분
            ResponseCookie accessCookie = createCookie("ACCESS_TOKEN",
                    tokenResponse.getAccessToken(),
                    accessTokenExpireSeconds,
                    true);

            long refreshTokenExpireSeconds = 24 * 60 * 60; // 1일
            ResponseCookie refreshCookie = createCookie("REFRESH_TOKEN",
                    tokenResponse.getRefreshToken(),
                    refreshTokenExpireSeconds,
                    true);

            response.addHeader("Set-Cookie", accessCookie.toString());
            response.addHeader("Set-Cookie", refreshCookie.toString());

            return ResponseEntity.ok("이메일 로그인 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 쿠키를 생성해줄 때 사용하는 헬퍼 메서드
     */
    private ResponseCookie createCookie(String name, String value, long maxAgeSeconds, boolean httpOnly) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)    // JS에서 쿠키 읽기 불가
                .secure(true)          // HTTPS 사용 시 true (로컬개발시엔 false)
                .path("/")
                .maxAge(maxAgeSeconds) // 쿠키 만료 (초 단위)
                .sameSite("None")      // Cross-site 사용 위해 "None" (프론트 서버가 다른 도메인일 경우)
                .build();
    }
}
