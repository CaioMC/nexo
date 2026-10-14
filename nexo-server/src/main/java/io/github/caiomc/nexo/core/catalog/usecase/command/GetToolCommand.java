package io.github.caiomc.nexo.core.catalog.usecase.command;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;

public record GetToolCommand(ToolName name) {
}
