package io.github.caiomc.nexo.core.catalog.port.repository;

import java.util.List;

import io.github.caiomc.nexo.core.catalog.port.repository.data.DependencyEdgeData;
import io.github.caiomc.nexo.core.catalog.port.repository.data.ToolData;
import io.github.caiomc.nexo.core.catalog.port.repository.data.ToolParameterData;

/**
 * Leitura crua do catálogo. Só {@code SELECT}s simples: montar o grafo e validar é trabalho do core.
 */
public interface ToolCatalogRepository {

    List<ToolData> tools();

    List<ToolParameterData> parameters();

    List<DependencyEdgeData> dependencies();
}
