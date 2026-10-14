package io.github.caiomc.nexo.adapters.catalog.web.response;

import java.util.List;

public record CatalogOverviewResponse(
        int contexts,
        int namespaces,
        int tools,
        int dependencies,
        List<ContextSummaryResponse> byContext
) {
}
