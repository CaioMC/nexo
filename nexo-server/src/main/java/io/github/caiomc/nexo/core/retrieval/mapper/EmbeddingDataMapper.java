package io.github.caiomc.nexo.core.retrieval.mapper;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.retrieval.domain.model.Embedding;
import io.github.caiomc.nexo.core.retrieval.domain.model.ToolDocument;
import io.github.caiomc.nexo.core.retrieval.port.embedding.command.EmbedTextsCommand;
import io.github.caiomc.nexo.core.retrieval.port.embedding.data.EmbeddingData;
import io.github.caiomc.nexo.core.retrieval.port.embedding.data.ToolEmbeddingData;

@Component
public class EmbeddingDataMapper {

    public EmbedTextsCommand toCommand(String text) {
        return new EmbedTextsCommand(List.of(text));
    }

    public EmbedTextsCommand toCommand(List<ToolDocument> documents) {
        return new EmbedTextsCommand(documents.stream().map(ToolDocument::content).toList());
    }

    public Embedding toEmbedding(EmbeddingData data) {
        return new Embedding(data.values());
    }

    public ToolEmbeddingData toData(
            ToolDocument document,
            String model,
            EmbeddingData embedding,
            Instant indexedAt
    ) {
        return new ToolEmbeddingData(
                document.toolName().value(),
                model,
                document.contentHash(),
                embedding.values(),
                indexedAt
        );
    }
}
