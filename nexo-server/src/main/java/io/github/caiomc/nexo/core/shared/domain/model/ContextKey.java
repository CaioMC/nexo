package io.github.caiomc.nexo.core.shared.domain.model;

import java.util.regex.Pattern;

import static java.util.Objects.isNull;

/**
 * Chave de um contexto Google ({@code gmail}, {@code calendar}...). É o único tipo, junto do
 * {@code UserId}, que os contextos delimitados compartilham.
 */
public record ContextKey(String value) implements Comparable<ContextKey> {

    private static final Pattern PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,31}$");

    public ContextKey {
        if (isNull(value) || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Contexto inválido: '" + value + "'. Use letras minúsculas, números e _, como 'calendar'.");
        }
    }

    public static ContextKey of(String value) {
        return new ContextKey(value);
    }

    @Override
    public int compareTo(ContextKey other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return this.value;
    }
}
