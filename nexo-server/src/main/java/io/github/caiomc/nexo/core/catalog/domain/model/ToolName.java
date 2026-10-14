package io.github.caiomc.nexo.core.catalog.domain.model;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;

/**
 * Nome de uma ferramenta, no formato que o modelo enxerga ({@code google_calendar_list_events}).
 * A ordem natural é alfabética: é ela que desempata o Kahn e deixa a saída determinística.
 */
public record ToolName(String value) implements Comparable<ToolName> {

    private static final Pattern PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");

    public ToolName {
        if (isNull(value) || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Nome de ferramenta inválido: '" + value + "'. Use snake_case, como 'google_calendar_list_events'.");
        }
    }

    public static ToolName of(String value) {
        return new ToolName(value);
    }

    public static List<String> values(Collection<ToolName> names) {
        return names.stream().map(ToolName::value).toList();
    }

    @Override
    public int compareTo(ToolName other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return this.value;
    }
}
