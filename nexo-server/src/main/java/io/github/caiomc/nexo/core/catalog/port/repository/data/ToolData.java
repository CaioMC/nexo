package io.github.caiomc.nexo.core.catalog.port.repository.data;

/**
 * Uma linha de {@code tool} com o contexto do namespace, como veio do banco.
 */
public record ToolData(
        String name,
        String namespace,
        String contextKey,
        String effect,
        String description
) {
}
