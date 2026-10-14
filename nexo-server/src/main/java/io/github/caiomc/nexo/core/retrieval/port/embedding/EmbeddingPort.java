package io.github.caiomc.nexo.core.retrieval.port.embedding;

import java.util.List;

import io.github.caiomc.nexo.core.retrieval.port.embedding.command.EmbedTextsCommand;
import io.github.caiomc.nexo.core.retrieval.port.embedding.data.EmbeddingData;

public interface EmbeddingPort {

    List<EmbeddingData> embed(EmbedTextsCommand command);

    String model();

    /**
     * Falso quando não há chave configurada: a busca cai para full-text.
     */
    boolean available();
}
