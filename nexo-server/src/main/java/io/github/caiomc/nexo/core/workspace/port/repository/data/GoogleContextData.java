package io.github.caiomc.nexo.core.workspace.port.repository.data;

/**
 * Uma linha de {@code google_context}.
 */
public record GoogleContextData(
        String key,
        String name,
        String description,
        String googleApi,
        int position
) {
}
