package io.github.caiomc.nexo.adapters.retrieval.persistence;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.retrieval.port.embedding.EmbeddingIndexRepository;
import io.github.caiomc.nexo.core.retrieval.port.embedding.data.IndexedEmbeddingData;
import io.github.caiomc.nexo.core.retrieval.port.embedding.data.ToolEmbeddingData;

@Component
public class PostgresEmbeddingIndexRepository implements EmbeddingIndexRepository {

    private final JdbcClient jdbc;

    public PostgresEmbeddingIndexRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<IndexedEmbeddingData> indexed(String model) {
        return this.jdbc.sql("SELECT tool_name, content_hash FROM tool_embedding WHERE model = :model")
                .param("model", model)
                .query((row, number) -> new IndexedEmbeddingData(row.getString("tool_name"), row.getString("content_hash")))
                .list();
    }

    @Override
    public void save(ToolEmbeddingData embedding) {
        this.jdbc.sql("""
                        INSERT INTO tool_embedding (tool_name, model, content_hash, embedding, indexed_at)
                        VALUES (:tool, :model, :hash, CAST(:embedding AS vector), :indexedAt)
                        ON CONFLICT (tool_name) DO UPDATE
                        SET model = EXCLUDED.model,
                            content_hash = EXCLUDED.content_hash,
                            embedding = EXCLUDED.embedding,
                            indexed_at = EXCLUDED.indexed_at
                        """)
                .param("tool", embedding.toolName())
                .param("model", embedding.model())
                .param("hash", embedding.contentHash())
                .param("embedding", PgVectors.literal(embedding.embedding()))
                .param("indexedAt", Timestamp.from(embedding.indexedAt()))
                .update();
    }
}
