package io.github.caiomc.nexo.core.retrieval.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.catalog.usecase.ProvideCatalogUseCase;
import io.github.caiomc.nexo.core.retrieval.domain.model.ToolDocument;
import io.github.caiomc.nexo.core.retrieval.domain.policy.ToolDocumentBuilder;
import io.github.caiomc.nexo.core.retrieval.mapper.EmbeddingDataMapper;
import io.github.caiomc.nexo.core.retrieval.port.embedding.EmbeddingIndexRepository;
import io.github.caiomc.nexo.core.retrieval.port.embedding.EmbeddingPort;
import io.github.caiomc.nexo.core.retrieval.port.embedding.data.EmbeddingData;
import io.github.caiomc.nexo.core.retrieval.port.embedding.data.IndexedEmbeddingData;
import io.github.caiomc.nexo.core.retrieval.usecase.IndexEmbeddingsUseCase;

/**
 * Mantém {@code tool_embedding} em dia: monta o documento de cada ferramenta, compara o hash com o
 * que está gravado para o modelo atual e gera vetor só para o que é novo ou mudou.
 */
@Service
public class IndexEmbeddingsService implements IndexEmbeddingsUseCase {

    private static final Logger log = LoggerFactory.getLogger(IndexEmbeddingsService.class);

    private final ProvideCatalogUseCase catalog;
    private final EmbeddingPort embeddings;
    private final EmbeddingIndexRepository index;

    private final ToolDocumentBuilder documents;
    private final EmbeddingDataMapper mapper;
    private final Clock clock;

    public IndexEmbeddingsService(
            ProvideCatalogUseCase catalog,
            EmbeddingPort embeddings,
            EmbeddingIndexRepository index,
            ToolDocumentBuilder documents,
            EmbeddingDataMapper mapper,
            Clock clock
    ) {
        this.catalog = catalog;
        this.embeddings = embeddings;
        this.index = index;
        this.documents = documents;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public int indexRAG() {
        if (!this.embeddings.available()) {
            log.warn("Sem chave de embeddings (OPENAI_API_KEY): índice vetorial não gerado, a busca usa só full-text");

            return 0;
        }

        String model = this.embeddings.model();
        Map<String, String> indexed = this.index.indexed(model)
                .stream()
                .collect(Collectors.toMap(IndexedEmbeddingData::toolName, IndexedEmbeddingData::contentHash));
        List<ToolDocument> pending = this.catalog.current()
                .tools()
                .stream()
                .map(this.documents::buildRAG)
                .filter(document -> !document.contentHash().equals(indexed.get(document.toolName().value())))
                .toList();

        if (pending.isEmpty()) {
            log.info("Índice vetorial em dia ({} ferramentas, modelo {})", indexed.size(), model);

            return 0;
        }

        List<EmbeddingData> vectors = this.embeddings.embed(this.mapper.toCommand(pending));
        Instant now = this.clock.instant();

        for (int i = 0; i < pending.size(); i++) {
            this.index.save(
                    this.mapper.toData(
                            pending.get(i),
                            model,
                            vectors.get(i),
                            now
                    )
            );
        }

        log.info("Índice vetorial: {} ferramentas (re)indexadas com {}", pending.size(), model);

        return pending.size();
    }
}
