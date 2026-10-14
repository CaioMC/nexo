package io.github.caiomc.nexo.core.conversation.port.tool.command;

import java.util.Map;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

/**
 * Executar uma ferramenta já admitida pelo {@code ToolGatekeeperService}, em nome do usuário.
 */
public record ExecuteToolCommand(
        UserId userId,
        ToolName tool,
        ContextKey context,
        Map<String, Object> arguments
) {
}
