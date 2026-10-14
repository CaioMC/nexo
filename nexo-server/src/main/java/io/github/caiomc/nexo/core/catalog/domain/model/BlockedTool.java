package io.github.caiomc.nexo.core.catalog.domain.model;

import java.util.List;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

/**
 * Ferramenta que não pode ser usada porque ela, ou algum pré-requisito dela, é de um contexto
 * desligado (RN-02).
 */
public record BlockedTool(
        ToolName name,
        List<ContextKey> missingContexts
) {

    public BlockedTool {
        missingContexts = List.copyOf(missingContexts);
    }
}
