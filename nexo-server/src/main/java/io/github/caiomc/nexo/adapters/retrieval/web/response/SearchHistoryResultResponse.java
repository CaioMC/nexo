package io.github.caiomc.nexo.adapters.retrieval.web.response;

import java.util.List;

public record SearchHistoryResultResponse(
        int step,
        String name,
        String origin,
        double score,
        Integer level,
        List<String> missingContexts
) {
}
