package io.github.caiomc.nexo.adapters.retrieval.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.github.caiomc.nexo.core.retrieval.domain.enums.RetrievalStrategy;

@ConfigurationProperties(prefix = "nexo.retrieval")
public record RetrievalProperties(
        RetrievalStrategy strategy,
        int topK,
        int candidates,
        int maxCandidates,
        int rrfK,
        boolean applyDag,
        int suggestionTop,
        boolean indexOnStartup
) {
}
