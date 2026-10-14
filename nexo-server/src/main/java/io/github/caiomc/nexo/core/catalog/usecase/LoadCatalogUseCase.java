package io.github.caiomc.nexo.core.catalog.usecase;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolCatalog;

/**
 * Lê o catálogo do banco, monta o grafo (recusando ciclo) e o deixa disponível em memória.
 */
public interface LoadCatalogUseCase {

    ToolCatalog load();
}
