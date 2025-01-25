package com.auction.back.global.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Slf4j
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        // 여기서 attributes에 있는 userEmail을 principal로 사용
        String email = (String) attributes.get("userEmail");
        if (email != null) {
            log.info("[CustomHandshakeHandler] Principal email = {}", email);

            // 간단히 Principal을 만들어 리턴 (익명 클래스 or 별도 클래스)
            return new Principal() {
                @Override
                public String getName() {
                    return email; // principal.getName() -> email
                }
            };
        }
        // else anonymous
        return super.determineUser(request, wsHandler, attributes);
    }
}
