package io.github.caiomc.nexo.core.retrieval.port.embedding.data;

/**
 * O que já está indexado no banco para um modelo: ferramenta e hash do texto que virou vetor.
 */
public record IndexedEmbeddingData(
        String toolName,
        String contentHash
) {
}
