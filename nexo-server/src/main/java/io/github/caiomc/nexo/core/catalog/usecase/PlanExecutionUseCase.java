package io.github.caiomc.nexo.core.catalog.usecase;

import io.github.caiomc.nexo.core.catalog.domain.model.ExecutionPlan;
import io.github.caiomc.nexo.core.catalog.usecase.command.PlanExecutionCommand;

public interface PlanExecutionUseCase {

    ExecutionPlan planDAG(PlanExecutionCommand command);
}
