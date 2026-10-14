package io.github.caiomc.nexo.core.workspace.port.repository.data;

/**
 * Uma linha de {@code google_context_scope}.
 */
public record GoogleContextScopeData(
        String contextKey,
        String scope
) {
}
