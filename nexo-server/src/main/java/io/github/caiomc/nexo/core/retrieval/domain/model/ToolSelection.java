package io.github.caiomc.nexo.core.retrieval.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.caiomc.nexo.core.catalog.domain.model.BlockedTool;
import io.github.caiomc.nexo.core.catalog.domain.model.ExecutionPlan;
import io.github.caiomc.nexo.core.catalog.domain.model.PlanStep;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.retrieval.domain.enums.RetrievalStrategy;
import io.github.caiomc.nexo.core.retrieval.domain.enums.SelectionOrigin;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * O que a busca entregou: as ferramentas (RAG + DAG, na ordem de execução quando o DAG foi
 * aplicado) e as bloqueadas por contexto desligado, que viram sugestão.
 *
 * @param searchId id em {@code tool_search}; nulo antes de gravar
 */
public record ToolSelection(
        Long searchId,
        String query,
        RetrievalStrategy strategy,
        int topK,
        boolean dagApplied,
        List<SelectedTool> tools,
        List<BlockedTool> blocked,
        long durationMs
) {

    public ToolSelection {
        tools = List.copyOf(tools);
        blocked = List.copyOf(blocked);
    }

    /**
     * Junta o RAG e o plano do DAG. Sem plano, as ferramentas ficam na ordem do RAG. Com plano,
     * ficam na ordem de execução; as que não estavam no RAG entram com origem DAG e nota 0.
     */
    public static ToolSelection of(
            String query,
            RetrievalStrategy strategy,
            int topK,
            List<RankedTool> rag,
            ExecutionPlan plan,
            List<BlockedTool> blocked,
            long durationMs
    ) {
        List<SelectedTool> tools = new ArrayList<>();

        if (isNull(plan)) {
            for (RankedTool ranked : rag) {
                tools.add(new SelectedTool(
                        tools.size() + 1,
                        ranked.name(),
                        SelectionOrigin.RAG,
                        ranked.score(),
                        null
                ));
            }
        } else {
            Map<ToolName, RankedTool> byName = rag.stream().collect(Collectors.toMap(RankedTool::name, Function.identity()));

            for (PlanStep step : plan.steps()) {
                RankedTool ranked = byName.get(step.tool());
                SelectionOrigin origin = isNull(ranked) ? SelectionOrigin.DAG : SelectionOrigin.RAG;
                double score = isNull(ranked) ? 0 : ranked.score();

                tools.add(new SelectedTool(
                        step.step(),
                        step.tool(),
                        origin,
                        score,
                        step.level()
                ));
            }
        }

        return new ToolSelection(
                null,
                query,
                strategy,
                topK,
                nonNull(plan),
                tools,
                blocked,
                durationMs
        );
    }

    public ToolSelection withSearchId(long id) {
        return new ToolSelection(
                id,
                this.query,
                this.strategy,
                this.topK,
                this.dagApplied,
                this.tools,
                this.blocked,
                this.durationMs
        );
    }

    public List<ToolName> names() {
        return this.tools.stream().map(SelectedTool::name).toList();
    }
}
