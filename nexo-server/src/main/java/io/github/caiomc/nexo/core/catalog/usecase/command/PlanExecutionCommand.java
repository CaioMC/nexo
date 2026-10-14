package io.github.caiomc.nexo.core.catalog.usecase.command;

import java.util.List;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;

public record PlanExecutionCommand(List<ToolName> tools) {

    public PlanExecutionCommand {
        if (tools.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos uma ferramenta para planejar a execução");
        }

        tools = List.copyOf(tools);
    }
}
