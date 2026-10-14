package io.github.caiomc.nexo.core.retrieval.domain.model;

import java.time.Instant;
import java.util.List;

import io.github.caiomc.nexo.core.retrieval.domain.enums.RetrievalStrategy;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

/**
 * Uma busca já feita, como ficou no histórico (auditoria).
 */
public record SearchRecord(
        long id,
        String query,
        RetrievalStrategy strategy,
        int topK,
        boolean dagApplied,
        List<ContextKey> allowedContexts,
        long durationMs,
        Instant createdAt,
        List<SearchRecordResult> results
) {
}
