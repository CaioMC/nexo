package io.github.caiomc.nexo.core.retrieval.domain.policy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.retrieval.domain.model.RankedTool;

/**
 * Reciprocal Rank Fusion: cada lista vota em seus candidatos com {@code 1 / (k + posição)} e a
 * nota final é a soma. Junta o que o lexical acerta (nomes exatos) com o que o semântico acerta
 * (sinônimos) sem precisar calibrar as notas de cada um. {@code k = 60} é o valor do artigo
 * original (Cormack et al., 2009) e o padrão de Elastic, OpenSearch e Azure AI Search.
 */
public final class ReciprocalRankFusion {

    private final int k;

    public ReciprocalRankFusion(int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("A constante k do RRF precisa ser positiva (nexo.retrieval.rrf-k)");
        }

        this.k = k;
    }

    public List<RankedTool> fuseRAG(
            List<ToolName> lexical,
            List<ToolName> semantic
    ) {
        Map<ToolName, Double> scores = new HashMap<>();

        this.vote(scores, lexical);
        this.vote(scores, semantic);

        return scores.entrySet()
                .stream()
                .map(entry -> new RankedTool(entry.getKey(), entry.getValue()))
                .sorted(RankedTool.BY_SCORE_THEN_NAME)
                .toList();
    }

    private void vote(
            Map<ToolName, Double> scores,
            List<ToolName> ranking
    ) {
        for (int position = 0; position < ranking.size(); position++) {
            scores.merge(ranking.get(position), 1.0 / (this.k + position + 1), Double::sum);
        }
    }
}
