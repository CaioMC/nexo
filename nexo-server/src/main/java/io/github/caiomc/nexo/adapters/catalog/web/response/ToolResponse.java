package io.github.caiomc.nexo.adapters.catalog.web.response;

import java.util.List;

public record ToolResponse(
        String name,
        String namespace,
        String context,
        String effect,
        String description,
        List<ToolParameterResponse> parameters,
        List<String> dependsOn,
        List<String> dependents,
        int level
) {
}
