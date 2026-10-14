package io.github.caiomc.nexo.adapters.retrieval.openai;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.caiomc.nexo.adapters.shared.openai.OpenAiHttpClient;
import io.github.caiomc.nexo.adapters.shared.openai.OpenAiSettings;
import io.github.caiomc.nexo.adapters.shared.openai.exception.OpenAiApiException;
import io.github.caiomc.nexo.core.retrieval.port.embedding.command.EmbedTextsCommand;
import io.github.caiomc.nexo.core.retrieval.port.embedding.data.EmbeddingData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O adapter contra um {@link HttpServer} do JDK no lugar do proxy: confere o que vai (modelo,
 * textos, chave no cabeçalho) e a reordenação pelo {@code index} do que volta.
 */
class OpenAiEmbeddingAdapterTest {

    private final ObjectMapper json = new ObjectMapper();
    private final List<JsonNode> requests = new ArrayList<>();
    private final List<String> authorizations = new ArrayList<>();

    private HttpServer server;
    private int status = 200;

    @BeforeEach
    void start() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/v1/embeddings", this::answer);
        this.server.start();
    }

    @AfterEach
    void stop() {
        this.server.stop(0);
    }

    @Test
    void sendsModelAndTextsAndReordersByIndex() {
        List<EmbeddingData> vectors = this.adapter("sk-teste").embed(new EmbedTextsCommand(List.of("um", "dois")));

        assertThat(vectors).extracting(vector -> vector.values()[0]).containsExactly(0.0, 1.0);
        assertThat(this.requests.getFirst().path("model").asText()).isEqualTo("text-embedding-3-small");
        assertThat(this.requests.getFirst().path("input")).hasSize(2);
        assertThat(this.authorizations).containsExactly("Bearer sk-teste");
    }

    @Test
    void errorsKeepTheStatus() {
        this.status = 401;

        assertThatThrownBy(() -> this.adapter("sk-errada").embed(new EmbedTextsCommand(List.of("um"))))
                .isInstanceOf(OpenAiApiException.class)
                .hasMessageContaining("401");
    }

    @Test
    void withoutKeyIsNotAvailable() {
        assertThat(this.adapter("").available()).isFalse();
        assertThat(this.adapter("sk-teste").available()).isTrue();
    }

    @Test
    void settingsNeverPrintTheKey() {
        assertThat(this.settings("sk-segredo").toString()).doesNotContain("sk-segredo").contains("apiKey=***");
    }

    private OpenAiEmbeddingAdapter adapter(String key) {
        OpenAiSettings settings = this.settings(key);

        return new OpenAiEmbeddingAdapter(new OpenAiHttpClient(settings, this.json), this.json, settings);
    }

    private OpenAiSettings settings(String key) {
        String baseUrl = "http://127.0.0.1:" + this.server.getAddress().getPort() + "/v1/";

        return new OpenAiSettings(
                baseUrl,
                key,
                "gpt-5.4-mini",
                "text-embedding-3-small",
                Duration.ofSeconds(5)
        );
    }

    private void answer(HttpExchange exchange) throws IOException {
        JsonNode request = this.json.readTree(exchange.getRequestBody());

        this.requests.add(request);
        this.authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));

        StringBuilder data = new StringBuilder();

        for (int i = request.path("input").size() - 1; i >= 0; i--) {
            data.append(data.isEmpty() ? "" : ",").append("{\"index\":").append(i).append(",\"embedding\":[").append(i).append(".0,0.5]}");
        }

        byte[] body = (this.status == 200 ? "{\"data\":[" + data + "]}" : "{\"error\":{\"message\":\"chave inválida\"}}").getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(this.status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
