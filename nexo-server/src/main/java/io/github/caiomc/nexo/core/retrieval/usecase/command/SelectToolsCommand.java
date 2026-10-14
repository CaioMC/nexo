package io.github.caiomc.nexo.core.retrieval.usecase.command;

import java.util.UUID;

import io.github.caiomc.nexo.core.retrieval.domain.enums.RetrievalStrategy;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Pedido de ferramentas.
 *
 * @param strategy nulo = padrão da configuração
 * @param topK     nulo = padrão da configuração
 * @param applyDag nulo = padrão da configuração
 * @param userId   de quem são os contextos que filtram a busca (RN-02); nulo = sem filtro
 *                 (avaliação offline do RAG)
 * @param turnId   turno que pediu a busca; nulo quando veio de {@code POST /api/search}
 */
public record SelectToolsCommand(
        String query,
        RetrievalStrategy strategy,
        Integer topK,
        Boolean applyDag,
        UserId userId,
        UUID turnId
) {

    public SelectToolsCommand {
        if (isNull(query) || query.isBlank()) {
            throw new IllegalArgumentException("Informe o pedido (query) para buscar ferramentas");
        }

        if (nonNull(topK) && (topK < 1 || topK > 20)) {
            throw new IllegalArgumentException("topK precisa estar entre 1 e 20, veio " + topK);
        }
    }
}
