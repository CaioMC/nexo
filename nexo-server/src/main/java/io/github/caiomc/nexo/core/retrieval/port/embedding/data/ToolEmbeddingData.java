package io.github.caiomc.nexo.core.retrieval.port.embedding.data;

import java.time.Instant;

/**
 * Uma linha de {@code tool_embedding}.
 */
public record ToolEmbeddingData(
        String toolName,
        String model,
        String contentHash,
        double[] embedding,
        Instant indexedAt
) {
}
