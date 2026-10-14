package io.github.caiomc.nexo.adapters.retrieval.persistence;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Vetor no formato de texto do pgvector ({@code [0.1,0.2,...]}). No SQL vira vetor com
 * {@code CAST(:embedding AS vector)}, sem biblioteca do pgvector no Java.
 */
final class PgVectors {

    private PgVectors() {
    }

    static String literal(double[] values) {
        return Arrays.stream(values)
                .mapToObj(Double::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
