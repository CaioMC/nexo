package io.github.caiomc.nexo.core.retrieval.port.history.data;

import java.util.List;

/**
 * Uma linha de {@code tool_search_result}.
 *
 * @param origin RAG, DAG ou BLOCKED
 */
public record ToolSearchResultData(
        int step,
        String toolName,
        String origin,
        double scoreRag,
        Integer dagLevel,
        List<String> missingContexts
) {
}
