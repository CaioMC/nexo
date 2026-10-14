package io.github.caiomc.nexo.adapters.conversation.websocket.message;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * {@code {"kind": "event", "seq": 42, ...}}: o mesmo formato no WebSocket e em
 * {@code GET /api/conversations/{id}/events}.
 */
public record EventMessage(
        String kind,
        long seq,
        UUID conversationId,
        UUID turnId,
        String type,
        Instant at,
        Map<String, Object> payload
) {

    public static final String EVENT = "event";
}
