package io.github.caiomc.nexo.adapters.conversation.websocket.handler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import io.github.caiomc.nexo.adapters.identity.config.IdentityProperties;

/**
 * {@code /ws/conversations/{id}}. Só a origem do front ({@code NEXO_WEB_ORIGIN}) abre o socket:
 * protege contra <i>cross-site WebSocket hijacking</i> (o cookie de sessão iria junto).
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final int MAX_MESSAGE_BYTES = 16 * 1024;

    private final ConversationWebSocketHandler handler;
    private final IdentityProperties identity;

    public WebSocketConfig(
            ConversationWebSocketHandler handler,
            IdentityProperties identity
    ) {
        this.handler = handler;
        this.identity = identity;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(this.handler, "/ws/conversations/*")
                .addInterceptors(new SessionHandshakeInterceptor())
                .setAllowedOrigins(this.identity.webOrigin());
    }

    /**
     * Mensagem do cliente com mais de 16 KiB fecha o socket (RNF de limites, doc 06).
     */
    @Bean
    ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();

        container.setMaxTextMessageBufferSize(MAX_MESSAGE_BYTES);

        return container;
    }
}
