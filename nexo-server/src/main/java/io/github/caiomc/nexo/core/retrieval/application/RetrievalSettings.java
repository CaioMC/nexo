package io.github.caiomc.nexo.core.retrieval.application;

import io.github.caiomc.nexo.core.retrieval.domain.enums.RetrievalStrategy;

import static java.util.Objects.isNull;

/**
 * Configuração da busca de ferramentas ({@code nexo.retrieval.*}).
 *
 * @param candidates    candidatos de cada lista antes da fusão
 * @param maxCandidates teto ao dobrar os candidatos quando o filtro de contextos derruba muitos
 * @param suggestionTop quantas das melhores no ranking geral viram sugestão de contexto
 */
public record RetrievalSettings(
        RetrievalStrategy strategy,
        int topK,
        int candidates,
        int maxCandidates,
        int rrfK,
        boolean applyDag,
        int suggestionTop
) {

    public RetrievalSettings {
        if (isNull(strategy)) {
            throw new IllegalArgumentException("Informe a estratégia padrão do RAG (nexo.retrieval.strategy)");
        }

        if (topK < 1 || candidates < topK || maxCandidates < candidates) {
            throw new IllegalArgumentException("Precisa valer 1 <= top-k <= candidates <= max-candidates (nexo.retrieval.*)");
        }
    }
}
