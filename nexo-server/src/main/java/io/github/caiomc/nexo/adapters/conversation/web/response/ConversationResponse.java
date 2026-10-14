package io.github.caiomc.nexo.adapters.conversation.web.response;

import java.time.Instant;
import java.util.UUID;

/**
 * @param activeTurn nulo quando não há turno em andamento
 */
public record ConversationResponse(
        UUID id,
        String title,
        long lastSeq,
        ActiveTurnResponse activeTurn,
        Instant createdAt,
        Instant updatedAt
) {
}
