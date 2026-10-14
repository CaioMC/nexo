package io.github.caiomc.nexo.core.conversation.port.repository.data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Uma linha de {@code tool_call}.
 */
public record ToolCallData(
        UUID turnId,
        String callId,
        String toolName,
        Map<String, Object> arguments,
        String status,
        String blockReason,
        List<String> missing,
        String outputSummary,
        String error,
        Instant startedAt,
        Instant finishedAt
) {
}
