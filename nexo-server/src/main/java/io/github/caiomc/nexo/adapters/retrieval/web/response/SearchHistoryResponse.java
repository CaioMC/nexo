package io.github.caiomc.nexo.adapters.retrieval.web.response;

import java.time.Instant;
import java.util.List;

public record SearchHistoryResponse(
        long searchId,
        String query,
        String strategy,
        int topK,
        boolean applyDag,
        List<String> allowedContexts,
        long durationMs,
        Instant createdAt,
        List<SearchHistoryResultResponse> results
) {
}
