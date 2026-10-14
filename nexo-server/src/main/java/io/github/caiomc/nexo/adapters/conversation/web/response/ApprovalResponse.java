package io.github.caiomc.nexo.adapters.conversation.web.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ApprovalResponse(
        UUID id,
        UUID turnId,
        String callId,
        String tool,
        String description,
        String severity,
        Map<String, Object> arguments,
        String status,
        Instant expiresAt,
        Instant decidedAt
) {
}
