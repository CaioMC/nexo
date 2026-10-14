package io.github.caiomc.nexo.core.retrieval.port.search.data;

/**
 * Candidata do full-text, como veio do banco ({@code ts_rank}: maior = mais parecida).
 */
public record LexicalCandidateData(
        String toolName,
        double rank
) {
}
