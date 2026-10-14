package io.github.caiomc.nexo.core.retrieval.port.search;

import java.util.List;

import io.github.caiomc.nexo.core.retrieval.domain.model.Embedding;
import io.github.caiomc.nexo.core.retrieval.port.search.data.LexicalCandidateData;
import io.github.caiomc.nexo.core.retrieval.port.search.data.VectorCandidateData;

/**
 * Candidatos crus do banco, usando os índices (GIN e HNSW). Sem filtro e sem fusão: a estratégia
 * fica no {@code SelectToolsService}.
 */
public interface ToolSearchPort {

    /**
     * @param terms termos da consulta full-text já montados ({@code a | b | c})
     */
    List<LexicalCandidateData> lexicalCandidatesRAG(
            String terms,
            int limit
    );

    List<VectorCandidateData> vectorCandidatesRAG(
            Embedding query,
            int limit
    );
}
