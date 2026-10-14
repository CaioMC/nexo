package io.github.caiomc.nexo.core.conversation.port.repository.data;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha de {@code conversation}.
 */
public record ConversationData(
        UUID id,
        UUID userId,
        String title,
        long lastSeq,
        Instant createdAt,
        Instant updatedAt
) {
}
