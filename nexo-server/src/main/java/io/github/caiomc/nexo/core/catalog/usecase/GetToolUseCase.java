package io.github.caiomc.nexo.core.catalog.usecase;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolDetail;
import io.github.caiomc.nexo.core.catalog.usecase.command.GetToolCommand;

public interface GetToolUseCase {

    ToolDetail get(GetToolCommand command);
}
