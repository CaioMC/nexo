package io.github.caiomc.nexo.core.catalog.port.repository.data;

/**
 * Uma linha de {@code tool_dependency}: {@code toolName} depende de {@code dependsOn}.
 */
public record DependencyEdgeData(
        String toolName,
        String dependsOn
) {
}
