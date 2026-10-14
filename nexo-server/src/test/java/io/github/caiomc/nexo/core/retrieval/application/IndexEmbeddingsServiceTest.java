package io.github.caiomc.nexo.core.retrieval.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import io.github.caiomc.nexo.core.retrieval.domain.policy.ToolDocumentBuilder;
import io.github.caiomc.nexo.core.retrieval.mapper.EmbeddingDataMapper;
import io.github.caiomc.nexo.core.retrieval.port.embedding.data.ToolEmbeddingData;
import io.github.caiomc.nexo.core.support.CatalogFixtures;
import io.github.caiomc.nexo.core.support.RetrievalTestDoubles.FakeEmbeddings;
import io.github.caiomc.nexo.core.support.RetrievalTestDoubles.FixedCatalog;
import io.github.caiomc.nexo.core.support.RetrievalTestDoubles.InMemoryEmbeddingIndex;

import static org.assertj.core.api.Assertions.assertThat;

class IndexEmbeddingsServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

    private final InMemoryEmbeddingIndex index = new InMemoryEmbeddingIndex();

    @Test
    void indexesEverythingOnceAndThenOnlyWhatChanged() {
        FakeEmbeddings embeddings = new FakeEmbeddings(true);
        IndexEmbeddingsService service = this.service(embeddings);

        assertThat(service.indexRAG()).isEqualTo(10);
        assertThat(service.indexRAG()).isZero();

        ToolEmbeddingData stale = this.index.rows.get("docs_read");

        this.index.rows.put(
                "docs_read",
                new ToolEmbeddingData(
                        stale.toolName(),
                        stale.model(),
                        "hash-antigo",
                        stale.embedding(),
                        stale.indexedAt()
                )
        );

        assertThat(service.indexRAG()).isEqualTo(1);
        assertThat(embeddings.requests).hasSize(2);
        assertThat(this.index.rows.get("docs_read").indexedAt()).isEqualTo(NOW);
    }

    @Test
    void withoutKeyDoesNothing() {
        assertThat(this.service(new FakeEmbeddings(false)).indexRAG()).isZero();
        assertThat(this.index.rows).isEmpty();
    }

    private IndexEmbeddingsService service(FakeEmbeddings embeddings) {
        return new IndexEmbeddingsService(
                new FixedCatalog(CatalogFixtures.catalog()),
                embeddings,
                this.index,
                new ToolDocumentBuilder(),
                new EmbeddingDataMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }
}
