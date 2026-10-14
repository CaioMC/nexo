package io.github.caiomc.nexo.core.catalog.domain.model;

import java.util.List;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

public record ContextSummary(
        ContextKey context,
        int tools,
        List<String> namespaces
) {

    public ContextSummary {
        namespaces = List.copyOf(namespaces);
    }
}
