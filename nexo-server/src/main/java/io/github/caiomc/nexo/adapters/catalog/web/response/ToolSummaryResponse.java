package io.github.caiomc.nexo.adapters.catalog.web.response;

public record ToolSummaryResponse(
        String name,
        String namespace,
        String context,
        String effect,
        String description
) {
}
