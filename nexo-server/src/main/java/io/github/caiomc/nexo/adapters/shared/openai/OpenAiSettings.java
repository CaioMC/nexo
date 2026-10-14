package io.github.caiomc.nexo.adapters.shared.openai;

import java.time.Duration;

import static java.util.Objects.isNull;

/**
 * Acesso à OpenAI ou a um servidor compatível com a API dela (na POC, o proxy de fronteira da DTA).
 *
 * <p>A chave pode faltar: a aplicação sobe sem ela e a busca usa só full-text. A chave nunca
 * aparece no {@link #toString()}.
 */
public record OpenAiSettings(
        String baseUrl,
        String apiKey,
        String model,
        String embeddingModel,
        Duration timeout
) {

    public OpenAiSettings {
        if (isNull(baseUrl) || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Informe a URL da API (OPENAI_BASE_URL / nexo.openai.base-url)");
        }

        if (isNull(timeout) || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("O timeout da OpenAI precisa ser positivo (OPENAI_TIMEOUT_SECONDS)");
        }

        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        apiKey = isNull(apiKey) ? "" : apiKey.strip();
    }

    public boolean hasApiKey() {
        return !this.apiKey.isEmpty();
    }

    @Override
    public String toString() {
        return "OpenAiSettings[baseUrl=" + this.baseUrl + ", model=" + this.model + ", embeddingModel=" + this.embeddingModel + ", apiKey=***]";
    }
}
