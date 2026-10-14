package io.github.caiomc.nexo.it;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolCatalog;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.catalog.usecase.ProvideCatalogUseCase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O seed (V8, gerado do JSON) carregado pelo CatalogLoaderService bate com docs/catalogo/google-tools.json:
 * mesmas ferramentas, efeitos, contextos, arestas e níveis calculados pelo DependencyGraph.
 */
class CatalogSeedIT extends PostgresIT {

    private static final Path JSON = Path.of("..", "docs", "catalogo", "google-tools.json");

    @Autowired
    private ProvideCatalogUseCase provider;

    @Autowired
    private ObjectMapper json;

    @Test
    void seedMatchesJson() throws IOException {
        JsonNode source = this.json.readTree(JSON.toFile());
        ToolCatalog catalog = this.provider.current();
        Map<String, String> contextByNamespace = new HashMap<>();
        int edges = 0;

        source.path("namespaces").forEach(namespace -> contextByNamespace.put(namespace.path("name").asText(), namespace.path("context").asText()));

        assertThat(catalog.size()).isEqualTo(source.path("tools").size()).isEqualTo(50);

        for (JsonNode expected : source.path("tools")) {
            ToolName name = ToolName.of(expected.path("name").asText());
            Tool tool = catalog.require(name);

            assertThat(tool.effect().name()).as("efeito de %s", name).isEqualTo(expected.path("effect").asText());
            assertThat(tool.context().value()).as("contexto de %s", name).isEqualTo(contextByNamespace.get(expected.path("namespace").asText()));
            assertThat(tool.parameters()).as("parâmetros de %s", name).hasSize(expected.path("parameters").size());
            assertThat(catalog.graph().levelOfDAG(name)).as("nível de %s", name).isEqualTo(expected.path("dag_level").asInt());
            assertThat(ToolName.values(catalog.graph().prerequisitesOfDAG(name))).as("depends_on de %s", name).containsExactlyElementsOf(this.texts(expected.path("depends_on")));
            edges += expected.path("depends_on").size();
        }

        assertThat(catalog.graph().edgeCount()).isEqualTo(edges).isEqualTo(33);
        assertThat(catalog.overview().contexts()).isEqualTo(8);
        assertThat(catalog.overview().namespaces()).isEqualTo(16);
    }

    private List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();

        array.forEach(value -> values.add(value.asText()));

        return values.stream().sorted().toList();
    }
}
