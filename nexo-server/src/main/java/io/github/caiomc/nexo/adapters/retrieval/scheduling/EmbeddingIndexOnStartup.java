package io.github.caiomc.nexo.adapters.retrieval.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.retrieval.usecase.IndexEmbeddingsUseCase;

/**
 * Gera os embeddings que faltam logo depois de carregar o catálogo. Falha (proxy fora do ar,
 * chave inválida) não derruba a aplicação: a busca continua com full-text.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnProperty(prefix = "nexo.retrieval", name = "index-on-startup", havingValue = "true", matchIfMissing = true)
public class EmbeddingIndexOnStartup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingIndexOnStartup.class);

    private final IndexEmbeddingsUseCase index;

    public EmbeddingIndexOnStartup(IndexEmbeddingsUseCase index) {
        this.index = index;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            this.index.indexRAG();
        } catch (RuntimeException e) {
            log.warn("Não foi possível gerar os embeddings na subida: {}", e.getMessage());
        }
    }
}
