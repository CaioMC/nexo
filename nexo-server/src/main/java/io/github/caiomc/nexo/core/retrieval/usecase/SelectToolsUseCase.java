package io.github.caiomc.nexo.core.retrieval.usecase;

import io.github.caiomc.nexo.core.retrieval.domain.model.ToolSelection;
import io.github.caiomc.nexo.core.retrieval.usecase.command.SelectToolsCommand;

public interface SelectToolsUseCase {

    ToolSelection selectRAGDAG(SelectToolsCommand command);
}
