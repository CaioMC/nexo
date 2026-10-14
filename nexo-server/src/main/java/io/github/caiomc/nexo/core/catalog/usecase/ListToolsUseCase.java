package io.github.caiomc.nexo.core.catalog.usecase;

import java.util.List;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.usecase.command.ListToolsCommand;

public interface ListToolsUseCase {

    List<Tool> list(ListToolsCommand command);
}
