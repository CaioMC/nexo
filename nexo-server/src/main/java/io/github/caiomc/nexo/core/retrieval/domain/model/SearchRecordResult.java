package io.github.caiomc.nexo.core.retrieval.domain.model;

import java.util.List;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

/**
 * @param origin RAG, DAG ou BLOCKED
 */
public record SearchRecordResult(
        int step,
        ToolName tool,
        String origin,
        double score,
        Integer level,
        List<ContextKey> missingContexts
) {
}
