package io.github.caiomc.nexo.core.retrieval.domain.model;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;

/**
 * O texto de uma ferramenta que vira vetor e o hash dele (para reindexar só o que mudou).
 */
public record ToolDocument(
        ToolName toolName,
        String content,
        String contentHash
) {
}
