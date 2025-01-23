package com.auction.back.global.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUtils {

    // 현재 인증된 사용자(Principal)의 이메일을 가져옴
    public static String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal().equals("anonymousUser")) {
            throw new RuntimeException("인증되지 않은 사용자입니다.");
        }
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        return userDetails.getUsername(); // email
    }
}
