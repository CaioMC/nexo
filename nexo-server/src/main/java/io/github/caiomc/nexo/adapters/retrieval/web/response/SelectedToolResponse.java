package io.github.caiomc.nexo.adapters.retrieval.web.response;

public record SelectedToolResponse(
        int step,
        String name,
        String origin,
        double score,
        Integer level
) {
}
