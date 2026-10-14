package io.github.caiomc.nexo.core.conversation.port.event.data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Uma linha de {@code conversation_event}.
 */
public record EventData(
        UUID conversationId,
        long seq,
        UUID turnId,
        String type,
        Map<String, Object> payload,
        Instant createdAt
) {
}
