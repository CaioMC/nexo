package io.github.caiomc.nexo.core.conversation.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import io.github.caiomc.nexo.core.conversation.domain.enums.EventType;

/**
 * Um evento gravado da conversa: o que o WebSocket entrega e o replay reproduz.
 */
public record AssistantEvent(
        UUID conversationId,
        long seq,
        UUID turnId,
        EventType type,
        Map<String, Object> payload,
        Instant at
) {
}
