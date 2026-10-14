package io.github.caiomc.nexo.adapters.catalog.scheduling;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.catalog.usecase.LoadCatalogUseCase;

/**
 * Carrega o catálogo na subida, antes da indexação de embeddings. Ciclo no grafo derruba a
 * subida com o caminho do ciclo na mensagem (RN-08).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CatalogLoadOnStartup implements ApplicationRunner {

    private final LoadCatalogUseCase load;

    public CatalogLoadOnStartup(LoadCatalogUseCase load) {
        this.load = load;
    }

    @Override
    public void run(ApplicationArguments args) {
        this.load.load();
    }
}
