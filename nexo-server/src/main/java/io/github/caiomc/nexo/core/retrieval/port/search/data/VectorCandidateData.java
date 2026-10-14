package io.github.caiomc.nexo.core.retrieval.port.search.data;

/**
 * Candidata do kNN, como veio do banco (distância de cosseno: menor = mais parecida).
 */
public record VectorCandidateData(
        String toolName,
        double distance
) {
}
