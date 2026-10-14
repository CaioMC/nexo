package io.github.caiomc.nexo.core.workspace.usecase;

import io.github.caiomc.nexo.core.workspace.domain.model.GoogleContext;
import io.github.caiomc.nexo.core.workspace.usecase.command.FindContextCommand;

public interface FindContextUseCase {

    GoogleContext find(FindContextCommand command);
}
