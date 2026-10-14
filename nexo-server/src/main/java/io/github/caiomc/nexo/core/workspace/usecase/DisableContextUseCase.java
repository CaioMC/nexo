package io.github.caiomc.nexo.core.workspace.usecase;

import io.github.caiomc.nexo.core.workspace.domain.model.ContextState;
import io.github.caiomc.nexo.core.workspace.usecase.command.DisableContextCommand;

public interface DisableContextUseCase {

    ContextState disable(DisableContextCommand command);
}
