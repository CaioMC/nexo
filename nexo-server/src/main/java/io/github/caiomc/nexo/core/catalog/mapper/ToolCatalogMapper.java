package io.github.caiomc.nexo.core.catalog.mapper;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.catalog.domain.model.DependencyGraph;
import io.github.caiomc.nexo.core.catalog.domain.model.ParameterType;
import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolCatalog;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolEffect;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolParameter;
import io.github.caiomc.nexo.core.catalog.port.repository.data.DependencyEdgeData;
import io.github.caiomc.nexo.core.catalog.port.repository.data.ToolData;
import io.github.caiomc.nexo.core.catalog.port.repository.data.ToolParameterData;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

/**
 * {@code ...Data} do banco → {@link ToolCatalog} + {@link DependencyGraph}. Montar o grafo já
 * valida a aciclicidade (RN-08).
 */
@Component
public class ToolCatalogMapper {

    public ToolCatalog toCatalog(
            List<ToolData> tools,
            List<ToolParameterData> parameters,
            List<DependencyEdgeData> dependencies
    ) {
        Map<String, List<ToolParameter>> parametersByTool = parameters.stream()
                .collect(Collectors.groupingBy(ToolParameterData::toolName, Collectors.mapping(this::toParameter, Collectors.toList())));
        List<Tool> catalog = tools.stream()
                .map(tool -> this.toTool(tool, parametersByTool.getOrDefault(tool.name(), List.of())))
                .toList();

        return new ToolCatalog(
                catalog,
                this.toGraph(catalog, dependencies)
        );
    }

    private DependencyGraph toGraph(
            List<Tool> tools,
            List<DependencyEdgeData> dependencies
    ) {
        Map<ToolName, List<ToolName>> prerequisites = dependencies.stream()
                .collect(Collectors.groupingBy(
                        edge -> ToolName.of(edge.toolName()),
                        Collectors.mapping(edge -> ToolName.of(edge.dependsOn()), Collectors.toList())
                ));

        return new DependencyGraph(
                tools.stream().map(Tool::name).toList(),
                prerequisites
        );
    }

    private Tool toTool(
            ToolData data,
            List<ToolParameter> parameters
    ) {
        return new Tool(
                ToolName.of(data.name()),
                data.namespace(),
                ContextKey.of(data.contextKey()),
                ToolEffect.valueOf(data.effect()),
                data.description(),
                parameters
        );
    }

    private ToolParameter toParameter(ToolParameterData data) {
        return new ToolParameter(
                data.ordinal(),
                data.name(),
                ParameterType.valueOf(data.type().toUpperCase(Locale.ROOT)),
                data.description(),
                data.required(),
                data.minimum(),
                data.maximum(),
                data.enumValues(),
                data.items()
        );
    }
}
