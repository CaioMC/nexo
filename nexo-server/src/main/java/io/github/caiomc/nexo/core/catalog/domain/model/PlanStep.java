package io.github.caiomc.nexo.core.catalog.domain.model;

import java.util.List;

/**
 * Um passo da ordem de execução.
 *
 * @param level         leva do Kahn (0 = sem pré-requisitos); ferramentas da mesma leva não
 *                      dependem umas das outras
 * @param prerequisites pré-requisitos diretos, em ordem alfabética
 */
public record PlanStep(
        int step,
        ToolName tool,
        int level,
        List<ToolName> prerequisites
) {

    public PlanStep {
        prerequisites = List.copyOf(prerequisites);
    }
}
