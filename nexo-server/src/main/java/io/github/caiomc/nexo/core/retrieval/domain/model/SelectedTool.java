package io.github.caiomc.nexo.core.retrieval.domain.model;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.retrieval.domain.enums.SelectionOrigin;

/**
 * Uma ferramenta entregue ao modelo.
 *
 * @param score nota do RAG (0 para as que vieram só pelo DAG)
 * @param level leva do Kahn; nulo quando o DAG não foi aplicado
 */
public record SelectedTool(
        int step,
        ToolName name,
        SelectionOrigin origin,
        double score,
        Integer level
) {
}
