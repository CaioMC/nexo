package io.github.caiomc.nexo.core.conversation.port.event.command;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Gravar um evento; o store dá o próximo {@code seq} da conversa.
 */
public record AppendEventCommand(
        UUID conversationId,
        UUID turnId,
        String type,
        Map<String, Object> payload,
        Instant at
) {
}
