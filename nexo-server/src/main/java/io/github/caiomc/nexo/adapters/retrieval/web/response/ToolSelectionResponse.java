package io.github.caiomc.nexo.adapters.retrieval.web.response;

import java.util.List;

public record ToolSelectionResponse(
        long searchId,
        String query,
        String strategy,
        int topK,
        boolean applyDag,
        long durationMs,
        List<SelectedToolResponse> tools,
        List<BlockedToolResponse> blocked
) {
}
