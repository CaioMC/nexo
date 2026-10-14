package io.github.caiomc.nexo.adapters.retrieval.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.caiomc.nexo.core.retrieval.application.RetrievalSettings;
import io.github.caiomc.nexo.core.retrieval.domain.policy.LexicalTermsBuilder;
import io.github.caiomc.nexo.core.retrieval.domain.policy.ReciprocalRankFusion;
import io.github.caiomc.nexo.core.retrieval.domain.policy.ToolDocumentBuilder;

/**
 * As políticas do RAG são classes puras do domínio (sem Spring); aqui viram beans.
 */
@Configuration
public class RetrievalConfig {

    @Bean
    RetrievalSettings retrievalSettings(RetrievalProperties properties) {
        return new RetrievalSettings(
                properties.strategy(),
                properties.topK(),
                properties.candidates(),
                properties.maxCandidates(),
                properties.rrfK(),
                properties.applyDag(),
                properties.suggestionTop()
        );
    }

    @Bean
    ReciprocalRankFusion reciprocalRankFusion(RetrievalSettings settings) {
        return new ReciprocalRankFusion(settings.rrfK());
    }

    @Bean
    LexicalTermsBuilder lexicalTermsBuilder() {
        return new LexicalTermsBuilder();
    }

    @Bean
    ToolDocumentBuilder toolDocumentBuilder() {
        return new ToolDocumentBuilder();
    }
}
