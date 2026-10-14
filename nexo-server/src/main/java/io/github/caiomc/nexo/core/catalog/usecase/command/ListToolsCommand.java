package io.github.caiomc.nexo.core.catalog.usecase.command;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

/**
 * @param context   nulo = todos os contextos
 * @param namespace nulo = todos os namespaces
 */
public record ListToolsCommand(
        ContextKey context,
        String namespace
) {
}
