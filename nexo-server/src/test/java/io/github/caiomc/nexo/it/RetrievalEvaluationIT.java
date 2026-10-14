package io.github.caiomc.nexo.it;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.retrieval.domain.enums.RetrievalStrategy;
import io.github.caiomc.nexo.core.retrieval.domain.model.ToolSelection;
import io.github.caiomc.nexo.core.retrieval.usecase.SelectToolsUseCase;
import io.github.caiomc.nexo.core.retrieval.usecase.command.SelectToolsCommand;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Recall do RAG lexical (sem rede) no conjunto de avaliação: o pedido em português precisa trazer a
 * ferramenta esperada entre as 5 primeiras. O híbrido com embeddings é medido com o proxy, à parte.
 */
class RetrievalEvaluationIT extends PostgresIT {

    private static final Logger log = LoggerFactory.getLogger(RetrievalEvaluationIT.class);
    private static final double MIN_RECALL = 0.70;

    @Autowired
    private SelectToolsUseCase select;

    @Autowired
    private ObjectMapper json;

    @Test
    void fullTextRecallAtFive() throws IOException {
        List<String> misses = new ArrayList<>();
        int hits = 0;
        int total = 0;

        try (InputStream input = this.getClass().getResourceAsStream("/retrieval/avaliacao.json")) {
            for (JsonNode item : this.json.readTree(input)) {
                String query = item.path("query").asText();
                ToolSelection selection = this.select.selectRAGDAG(new SelectToolsCommand(query, RetrievalStrategy.FULL_TEXT, 5, false, null, null));
                List<String> found = ToolName.values(selection.names());

                for (JsonNode expected : item.path("expected")) {
                    total++;

                    if (found.contains(expected.asText())) {
                        hits++;
                    } else {
                        misses.add(query + " → esperava " + expected.asText() + ", veio " + found);
                    }
                }
            }
        }

        double recall = (double) hits / total;

        log.info("Recall@5 FULL_TEXT: {}/{} = {}", hits, total, String.format("%.2f", recall));
        misses.forEach(miss -> log.info("  errou: {}", miss));

        assertThat(recall).as("Recall@5 do full-text; erros: %s", misses).isGreaterThanOrEqualTo(MIN_RECALL);
    }
}
