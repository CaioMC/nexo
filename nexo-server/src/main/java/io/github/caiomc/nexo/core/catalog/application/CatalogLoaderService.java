package io.github.caiomc.nexo.core.catalog.application;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolCatalog;
import io.github.caiomc.nexo.core.catalog.mapper.ToolCatalogMapper;
import io.github.caiomc.nexo.core.catalog.port.repository.ToolCatalogRepository;
import io.github.caiomc.nexo.core.catalog.usecase.LoadCatalogUseCase;
import io.github.caiomc.nexo.core.catalog.usecase.ProvideCatalogUseCase;

import static java.util.Objects.isNull;

/**
 * Lê o catálogo uma vez (na subida) e o mantém em memória. O catálogo só muda por migration,
 * então não há motivo para ir ao banco a cada busca.
 */
@Service
public class CatalogLoaderService implements LoadCatalogUseCase, ProvideCatalogUseCase {

    private static final Logger log = LoggerFactory.getLogger(CatalogLoaderService.class);

    private final ToolCatalogRepository repository;
    private final ToolCatalogMapper mapper;

    private final AtomicReference<ToolCatalog> current = new AtomicReference<>();

    public CatalogLoaderService(
            ToolCatalogRepository repository,
            ToolCatalogMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ToolCatalog load() {
        ToolCatalog catalog = this.mapper.toCatalog(
                this.repository.tools(),
                this.repository.parameters(),
                this.repository.dependencies()
        );

        this.current.set(catalog);
        log.info("Catálogo carregado: {} ferramentas, {} dependências, sem ciclos", catalog.size(), catalog.graph().edgeCount());

        return catalog;
    }

    @Override
    public ToolCatalog current() {
        ToolCatalog catalog = this.current.get();

        if (isNull(catalog)) {
            return this.load();
        }

        return catalog;
    }
}
