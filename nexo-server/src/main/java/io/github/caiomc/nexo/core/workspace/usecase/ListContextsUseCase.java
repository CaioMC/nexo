package io.github.caiomc.nexo.core.workspace.usecase;

import java.util.List;

import io.github.caiomc.nexo.core.workspace.domain.model.ContextState;
import io.github.caiomc.nexo.core.workspace.usecase.command.ListContextsCommand;

public interface ListContextsUseCase {

    List<ContextState> list(ListContextsCommand command);
}
