package io.github.caiomc.nexo.adapters.conversation.websocket.handler;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import io.github.caiomc.nexo.adapters.identity.security.SessionUser;

/**
 * Sem sessão, sem upgrade: recusa com HTTP 401 antes de abrir o WebSocket (doc 06, seção 1). A
 * origem é conferida pelo registro do handler ({@code setAllowedOrigins}).
 */
public class SessionHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER = "nexo.user";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler handler,
            Map<String, Object> attributes
    ) {
        Principal principal = request.getPrincipal();

        if (principal instanceof Authentication authentication && authentication.getPrincipal() instanceof SessionUser user) {
            attributes.put(USER, user);

            return true;
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);

        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler handler,
            Exception exception
    ) {
    }
}
