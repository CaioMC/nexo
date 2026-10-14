package io.github.caiomc.nexo.core.conversation.port.repository.data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Uma linha de {@code turn} (o usuário vem da conversa).
 */
public record TurnData(
        UUID id,
        UUID conversationId,
        UUID userId,
        UUID clientMessageId,
        String userMessage,
        String assistantMessage,
        String status,
        String failureCode,
        String model,
        List<String> enabledContexts,
        int modelCalls,
        long inputTokens,
        long cachedInputTokens,
        long outputTokens,
        Instant startedAt,
        Instant finishedAt
) {
}
