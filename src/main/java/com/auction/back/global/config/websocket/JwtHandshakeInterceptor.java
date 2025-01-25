package com.auction.back.global.config.websocket;

import com.auction.back.global.jwt.TokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final TokenProvider tokenProvider;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) throws Exception {

        // 1) 쿠키에서 ACCESS_TOKEN 찾기
        if (request instanceof org.springframework.http.server.ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            Cookie[] cookies = httpServletRequest.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if ("ACCESS_TOKEN".equals(c.getName())) {
                        String token = c.getValue();
                        if (tokenProvider.validateToken(token)) {
                            String email = tokenProvider.getEmailFromToken(token);
                            // Principal 이름 = email
                            // attributes에 저장
                            attributes.put("userEmail", email);
                            log.info("[HandshakeInterceptor] userEmail = {}", email);
                        } else {
                            log.warn("[HandshakeInterceptor] Invalid token in cookie");
                        }
                    }
                }
            }
        }

        return true; // handShake 진행
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception ex
    ) {
        // Not used
    }
}
