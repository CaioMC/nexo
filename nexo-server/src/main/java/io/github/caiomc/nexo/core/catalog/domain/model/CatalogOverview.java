package io.github.caiomc.nexo.core.catalog.domain.model;

import java.util.List;

public record CatalogOverview(
        int contexts,
        int namespaces,
        int tools,
        int dependencies,
        List<ContextSummary> byContext
) {

    public CatalogOverview {
        byContext = List.copyOf(byContext);
    }
}
