package io.github.caiomc.nexo.core.catalog.application;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.catalog.domain.model.ExecutionPlan;
import io.github.caiomc.nexo.core.catalog.usecase.PlanExecutionUseCase;
import io.github.caiomc.nexo.core.catalog.usecase.ProvideCatalogUseCase;
import io.github.caiomc.nexo.core.catalog.usecase.command.PlanExecutionCommand;

@Service
public class PlanExecutionService implements PlanExecutionUseCase {

    private final ProvideCatalogUseCase catalog;

    public PlanExecutionService(ProvideCatalogUseCase catalog) {
        this.catalog = catalog;
    }

    @Override
    public ExecutionPlan planDAG(PlanExecutionCommand command) {
        return this.catalog.current().graph().executionOrderDAG(command.tools());
    }
}
