package io.github.caiomc.nexo.core.retrieval.port.embedding;

import java.util.List;

import io.github.caiomc.nexo.core.retrieval.port.embedding.data.IndexedEmbeddingData;
import io.github.caiomc.nexo.core.retrieval.port.embedding.data.ToolEmbeddingData;

public interface EmbeddingIndexRepository {

    List<IndexedEmbeddingData> indexed(String model);

    void save(ToolEmbeddingData embedding);
}
