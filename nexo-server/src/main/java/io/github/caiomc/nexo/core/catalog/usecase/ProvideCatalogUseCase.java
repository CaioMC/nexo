package io.github.caiomc.nexo.core.catalog.usecase;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolCatalog;

/**
 * O catálogo carregado. É por aqui que os outros contextos (retrieval, conversation) o enxergam.
 */
public interface ProvideCatalogUseCase {

    ToolCatalog current();
}
