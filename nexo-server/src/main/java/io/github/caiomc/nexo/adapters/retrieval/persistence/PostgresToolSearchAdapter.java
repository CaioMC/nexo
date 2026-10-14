package io.github.caiomc.nexo.adapters.retrieval.persistence;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.retrieval.domain.model.Embedding;
import io.github.caiomc.nexo.core.retrieval.port.search.ToolSearchPort;
import io.github.caiomc.nexo.core.retrieval.port.search.data.LexicalCandidateData;
import io.github.caiomc.nexo.core.retrieval.port.search.data.VectorCandidateData;

/**
 * Os dois {@code SELECT}s de candidatos do RAG. Usam os índices (GIN e HNSW) e nada mais: sem
 * filtro de contexto, sem fusão, sem DAG. Dá para rodar o mesmo SQL no psql e ver o mesmo resultado.
 */
@Component
public class PostgresToolSearchAdapter implements ToolSearchPort {

    private final JdbcClient jdbc;

    public PostgresToolSearchAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<LexicalCandidateData> lexicalCandidatesRAG(
            String terms,
            int limit
    ) {
        return this.jdbc.sql("""
                        SELECT t.name, ts_rank(t.search_vector, q) AS rank
                        FROM tool t, to_tsquery('pt_unaccent'::regconfig, :terms) q
                        WHERE t.search_vector @@ q
                        ORDER BY rank DESC, t.name
                        LIMIT :limit
                        """)
                .param("terms", terms)
                .param("limit", limit)
                .query((row, number) -> new LexicalCandidateData(row.getString("name"), row.getDouble("rank")))
                .list();
    }

    @Override
    public List<VectorCandidateData> vectorCandidatesRAG(
            Embedding query,
            int limit
    ) {
        return this.jdbc.sql("""
                        SELECT tool_name, embedding <=> CAST(:embedding AS vector) AS distance
                        FROM tool_embedding
                        ORDER BY distance, tool_name
                        LIMIT :limit
                        """)
                .param("embedding", PgVectors.literal(query.values()))
                .param("limit", limit)
                .query((row, number) -> new VectorCandidateData(row.getString("tool_name"), row.getDouble("distance")))
                .list();
    }
}
