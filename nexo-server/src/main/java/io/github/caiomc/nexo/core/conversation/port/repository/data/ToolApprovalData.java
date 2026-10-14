package io.github.caiomc.nexo.core.conversation.port.repository.data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Uma linha de {@code tool_approval}, com a conversa do turno.
 */
public record ToolApprovalData(
        UUID id,
        UUID userId,
        UUID conversationId,
        UUID turnId,
        String callId,
        String toolName,
        String description,
        String severity,
        Map<String, Object> arguments,
        String argumentsHash,
        String status,
        Instant createdAt,
        Instant expiresAt,
        Instant decidedAt
) {
}
