package io.github.caiomc.nexo.adapters.shared.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code nexo.openai.*}, preenchido por OPENAI_* do ambiente ou do .env.
 *
 * @param apiKey         vazia = sem modelo nem embeddings (busca só full-text)
 * @param embeddingModel precisa ter 1536 dimensões (coluna vector(1536))
 */
@ConfigurationProperties(prefix = "nexo.openai")
public record OpenAiProperties(
        String baseUrl,
        String apiKey,
        String model,
        String embeddingModel,
        Duration timeout
) {
}
