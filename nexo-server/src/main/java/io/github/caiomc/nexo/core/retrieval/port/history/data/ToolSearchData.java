package io.github.caiomc.nexo.core.retrieval.port.history.data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Uma linha de {@code tool_search} com os resultados.
 *
 * @param id     nulo ao gravar (o banco gera)
 * @param userId nulo na busca sem usuário
 * @param turnId nulo fora de um turno
 */
public record ToolSearchData(
        Long id,
        UUID userId,
        UUID turnId,
        String query,
        String strategy,
        int topK,
        boolean dagApplied,
        List<String> allowedContexts,
        long durationMs,
        Instant createdAt,
        List<ToolSearchResultData> results
) {
}
