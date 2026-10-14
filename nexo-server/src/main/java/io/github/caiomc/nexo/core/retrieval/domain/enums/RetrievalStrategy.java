package io.github.caiomc.nexo.core.retrieval.domain.enums;

/**
 * Como o RAG encontra as ferramentas parecidas com o pedido.
 */
public enum RetrievalStrategy {

    /** Só o full-text (GIN, português sem acento). Não precisa de chave. */
    FULL_TEXT,

    /** Só a similaridade de cosseno dos embeddings (HNSW). */
    VECTOR,

    /** As duas listas fundidas por Reciprocal Rank Fusion. */
    HYBRID;

    public boolean needsEmbedding() {
        return this != FULL_TEXT;
    }
}
