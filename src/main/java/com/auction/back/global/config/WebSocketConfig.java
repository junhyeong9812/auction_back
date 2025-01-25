package com.auction.back.global.config;

import com.auction.back.global.config.websocket.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final HandshakeHandler handshakeHandler;
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // /topic, /queue 로 브로드캐스트용
        config.enableSimpleBroker("/topic", "/queue");

        // 클라이언트 → 서버: /app/... 로 전송
        config.setApplicationDestinationPrefixes("/app");

        // **개인 채널** 접두어 설정 (기본 "/user")
        config.setUserDestinationPrefix("/user");

    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 웹소켓 연결 엔드포인트, SockJS 지원
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(handshakeHandler)            // (1) HandshakeHandler
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS();
    }
}
