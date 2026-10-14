package io.github.caiomc.nexo.core.workspace.usecase;

import io.github.caiomc.nexo.core.workspace.domain.model.ContextActivation;
import io.github.caiomc.nexo.core.workspace.usecase.command.EnableContextCommand;

public interface EnableContextUseCase {

    ContextActivation enable(EnableContextCommand command);
}
