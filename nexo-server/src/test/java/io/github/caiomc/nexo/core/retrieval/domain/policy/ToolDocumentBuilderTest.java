package io.github.caiomc.nexo.core.retrieval.domain.policy;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.caiomc.nexo.core.catalog.domain.model.ParameterType;
import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolEffect;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolParameter;
import io.github.caiomc.nexo.core.retrieval.domain.model.ToolDocument;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

import static org.assertj.core.api.Assertions.assertThat;

class ToolDocumentBuilderTest {

    private final ToolDocumentBuilder builder = new ToolDocumentBuilder();

    @Test
    void contentHasNameNamespaceDescriptionAndParameters() {
        ToolDocument document = this.builder.buildRAG(this.tool("Lê o texto de um documento."));

        assertThat(document.content()).isEqualTo("google docs read google docs document Lê o texto de um documento. documentId");
        assertThat(document.contentHash()).hasSize(64);
    }

    @Test
    void hashChangesWhenTheTextChanges() {
        assertThat(this.builder.buildRAG(this.tool("Uma descrição.")).contentHash())
                .isEqualTo(this.builder.buildRAG(this.tool("Uma descrição.")).contentHash())
                .isNotEqualTo(this.builder.buildRAG(this.tool("Outra descrição.")).contentHash());
    }

    private Tool tool(String description) {
        return new Tool(
                ToolName.of("google_docs_read"),
                "google_docs_document",
                ContextKey.of("docs"),
                ToolEffect.READ,
                description,
                List.of(new ToolParameter(1, "documentId", ParameterType.STRING, "Id.", true, null, null, null, null))
        );
    }
}
