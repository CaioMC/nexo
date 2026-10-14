package io.github.caiomc.nexo.core.catalog.mapper;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.caiomc.nexo.core.catalog.domain.exception.DependencyCycleException;
import io.github.caiomc.nexo.core.catalog.domain.model.ParameterType;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolCatalog;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolEffect;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.catalog.port.repository.data.DependencyEdgeData;
import io.github.caiomc.nexo.core.catalog.port.repository.data.ToolData;
import io.github.caiomc.nexo.core.catalog.port.repository.data.ToolParameterData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolCatalogMapperTest {

    private final ToolCatalogMapper mapper = new ToolCatalogMapper();

    private final List<ToolData> tools = List.of(
            new ToolData("google_docs_read", "google_docs_document", "docs", "READ", "Lê o texto de um documento."),
            new ToolData("google_docs_append_text", "google_docs_document", "docs", "WRITE", "Acrescenta texto no fim.")
    );

    @Test
    void mapsDataToDomainWithParametersInOrder() {
        ToolCatalog catalog = this.mapper.toCatalog(
                this.tools,
                List.of(
                        new ToolParameterData("google_docs_append_text", 2, "text", "string", "Texto.", true, null, null, null, null),
                        new ToolParameterData("google_docs_append_text", 1, "documentId", "string", "Id.", true, null, null, null, null),
                        new ToolParameterData("google_docs_read", 1, "ids", "array", "Ids.", true, null, null, null, "{\"type\": \"string\"}")
                ),
                List.of(new DependencyEdgeData("google_docs_append_text", "google_docs_read"))
        );

        assertThat(catalog.require(ToolName.of("google_docs_append_text")).effect()).isEqualTo(ToolEffect.WRITE);
        assertThat(catalog.require(ToolName.of("google_docs_append_text")).parameters())
                .extracting(parameter -> parameter.name())
                .containsExactly("documentId", "text");
        assertThat(catalog.require(ToolName.of("google_docs_read")).parameters().getFirst().type()).isEqualTo(ParameterType.ARRAY);
        assertThat(catalog.graph().prerequisitesOfDAG(ToolName.of("google_docs_append_text"))).containsExactly(ToolName.of("google_docs_read"));
    }

    @Test
    void cycleInTheDataStopsTheLoad() {
        assertThatThrownBy(() -> this.mapper.toCatalog(
                this.tools,
                List.of(),
                List.of(
                        new DependencyEdgeData("google_docs_append_text", "google_docs_read"),
                        new DependencyEdgeData("google_docs_read", "google_docs_append_text")
                )
        )).isInstanceOf(DependencyCycleException.class);
    }
}
