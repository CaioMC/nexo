package io.github.caiomc.nexo.core.retrieval.domain.model;

import java.util.Comparator;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;

/**
 * Uma ferramenta na lista do RAG com a nota da estratégia usada. Notas de estratégias
 * diferentes têm escalas diferentes: compare a ordem, não o número.
 */
public record RankedTool(
        ToolName name,
        double score
) {

    public static final Comparator<RankedTool> BY_SCORE_THEN_NAME = Comparator
            .comparingDouble(RankedTool::score)
            .reversed()
            .thenComparing(RankedTool::name);
}
