package io.github.caiomc.nexo.it;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import io.github.caiomc.nexo.adapters.retrieval.persistence.PostgresEmbeddingIndexRepository;
import io.github.caiomc.nexo.adapters.retrieval.persistence.PostgresToolSearchAdapter;
import io.github.caiomc.nexo.core.retrieval.domain.model.Embedding;
import io.github.caiomc.nexo.core.retrieval.port.embedding.data.ToolEmbeddingData;
import io.github.caiomc.nexo.core.retrieval.port.search.data.LexicalCandidateData;
import io.github.caiomc.nexo.core.retrieval.port.search.data.VectorCandidateData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Os adapters só devolvem candidatos crus, ordenados pelo índice, sem filtro de contexto.
 */
class PostgresToolSearchAdapterIT extends PostgresIT {

    private static final int DIMENSIONS = 1536;

    @Autowired
    private PostgresToolSearchAdapter search;

    @Autowired
    private PostgresEmbeddingIndexRepository index;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void lexicalCandidatesUsePortugueseWithoutAccents() {
        List<LexicalCandidateData> candidates = this.search.lexicalCandidatesRAG("planilha | celula | valores", 5);

        assertThat(candidates).isNotEmpty();
        assertThat(candidates).extracting(LexicalCandidateData::toolName).allMatch(name -> name.startsWith("google_sheets_"));
        assertThat(candidates).extracting(LexicalCandidateData::rank).isSortedAccordingTo((a, b) -> Double.compare(b, a));
    }

    @Test
    void lexicalCandidatesAreNotFilteredByContext() {
        assertThat(this.search.lexicalCandidatesRAG("gmail | agenda", 50))
                .extracting(LexicalCandidateData::toolName)
                .anyMatch(name -> name.startsWith("google_gmail_"))
                .anyMatch(name -> name.startsWith("google_calendar_"));
    }

    @Test
    void vectorCandidatesComeByCosineDistance() {
        this.jdbc.sql("DELETE FROM tool_embedding").update();
        this.save("google_docs_read", this.axis(0));
        this.save("google_sheets_get_values", this.axis(1));
        this.save("google_calendar_list_events", this.mix(0, 1));

        List<VectorCandidateData> candidates = this.search.vectorCandidatesRAG(new Embedding(this.axis(0)), 3);

        assertThat(candidates).extracting(VectorCandidateData::toolName)
                .containsExactly("google_docs_read", "google_calendar_list_events", "google_sheets_get_values");
        assertThat(candidates.getFirst().distance()).isCloseTo(0, offset(1e-6));
        assertThat(this.index.indexed("modelo-teste")).hasSize(3);
    }

    private void save(
            String tool,
            double[] vector
    ) {
        String hash = "hash-" + tool;

        this.index.save(new ToolEmbeddingData(
                tool,
                "modelo-teste",
                hash,
                vector,
                Instant.parse("2026-10-13T12:00:00Z")
        ));
    }

    private double[] axis(int position) {
        double[] vector = new double[DIMENSIONS];

        vector[position] = 1;

        return vector;
    }

    private double[] mix(
            int first,
            int second
    ) {
        double[] vector = new double[DIMENSIONS];

        vector[first] = 0.8;
        vector[second] = 0.6;

        return vector;
    }
}
