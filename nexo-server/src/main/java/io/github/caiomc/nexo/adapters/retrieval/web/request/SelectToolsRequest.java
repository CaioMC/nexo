package io.github.caiomc.nexo.adapters.retrieval.web.request;

public record SelectToolsRequest(
        String query,
        String strategy,
        Integer topK,
        Boolean applyDag
) {
}
