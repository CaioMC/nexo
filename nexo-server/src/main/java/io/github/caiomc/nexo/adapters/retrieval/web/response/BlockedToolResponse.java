package io.github.caiomc.nexo.adapters.retrieval.web.response;

import java.util.List;

public record BlockedToolResponse(
        String name,
        List<String> missingContexts
) {
}
