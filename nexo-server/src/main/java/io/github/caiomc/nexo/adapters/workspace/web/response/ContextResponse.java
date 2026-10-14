package io.github.caiomc.nexo.adapters.workspace.web.response;

import java.util.List;

public record ContextResponse(
        String key,
        String name,
        String description,
        boolean enabled,
        boolean scopesGranted,
        List<String> scopes,
        int toolCount
) {
}
