package io.github.caiomc.nexo.core.catalog.domain.model;

import java.util.List;

/**
 * As ferramentas pedidas mais todos os pré-requisitos, na ordem em que precisam rodar.
 */
public record ExecutionPlan(
        List<ToolName> requested,
        List<PlanStep> steps
) {

    public ExecutionPlan {
        requested = List.copyOf(requested);
        steps = List.copyOf(steps);
    }

    public List<ToolName> tools() {
        return this.steps.stream().map(PlanStep::tool).toList();
    }
}
