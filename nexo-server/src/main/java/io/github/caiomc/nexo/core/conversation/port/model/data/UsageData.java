package io.github.caiomc.nexo.core.conversation.port.model.data;

/**
 * Tokens gastos numa chamada ao modelo.
 */
public record UsageData(
        long inputTokens,
        long cachedInputTokens,
        long outputTokens
) {
}
