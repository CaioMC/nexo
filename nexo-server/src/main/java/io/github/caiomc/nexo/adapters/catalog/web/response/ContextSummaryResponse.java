package io.github.caiomc.nexo.adapters.catalog.web.response;

import java.util.List;

public record ContextSummaryResponse(
        String context,
        int tools,
        List<String> namespaces
) {
}
