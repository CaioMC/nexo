package io.github.caiomc.nexo.core.retrieval.domain.model;

import java.util.Arrays;

import static java.util.Objects.isNull;

/**
 * Um vetor de embedding. Imutável: o array é copiado na entrada e na saída.
 */
public record Embedding(double[] values) {

    public Embedding {
        if (isNull(values) || values.length == 0) {
            throw new IllegalArgumentException("Embedding vazio");
        }

        values = values.clone();
    }

    @Override
    public double[] values() {
        return this.values.clone();
    }

    public int dimensions() {
        return this.values.length;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Embedding embedding && Arrays.equals(this.values, embedding.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.values);
    }

    @Override
    public String toString() {
        return "Embedding[" + this.values.length + " dimensões]";
    }
}
