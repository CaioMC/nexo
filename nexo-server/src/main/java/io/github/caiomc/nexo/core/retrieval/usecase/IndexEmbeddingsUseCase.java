package io.github.caiomc.nexo.core.retrieval.usecase;

public interface IndexEmbeddingsUseCase {

    /**
     * Gera os vetores que faltam (ferramenta nova, texto alterado ou outro modelo).
     *
     * @return quantas ferramentas foram (re)indexadas
     */
    int indexRAG();
}
