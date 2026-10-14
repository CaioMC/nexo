package io.github.caiomc.nexo.adapters.shared.openai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.caiomc.nexo.adapters.shared.openai.exception.OpenAiApiException;
import io.github.caiomc.nexo.adapters.shared.openai.exception.OpenAiNotConfiguredException;

/**
 * POST de JSON na API compatível com a OpenAI usando o {@link HttpClient} do JDK, como no
 * tool-search-pg. Sem SDK de propósito: dá para ver exatamente o que vai e o que volta.
 * {@link #stream} lê respostas em Server-Sent Events ({@code "stream": true}).
 */
public class OpenAiHttpClient {

    private final OpenAiSettings settings;
    private final ObjectMapper json;
    private final HttpClient http;

    public OpenAiHttpClient(
            OpenAiSettings settings,
            ObjectMapper json
    ) {
        this.settings = settings;
        this.json = json;
        this.http = HttpClient
                .newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(settings.timeout())
                .build();
    }

    public boolean configured() {
        return this.settings.hasApiKey();
    }

    public JsonNode post(
            String path,
            JsonNode body
    ) {
        if (!this.settings.hasApiKey()) {
            throw new OpenAiNotConfiguredException();
        }

        HttpRequest request = HttpRequest
                .newBuilder(URI.create(this.settings.baseUrl() + path))
                .timeout(this.settings.timeout())
                .header("Authorization", "Bearer " + this.settings.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(this.write(body)))
                .build();

        try {
            HttpResponse<String> response = this.http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                throw new OpenAiApiException(response.statusCode(), path, response.body());
            }

            return this.json.readTree(response.body());
        } catch (IOException e) {
            throw new UncheckedIOException("Falha de comunicação com " + this.settings.baseUrl() + path + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chamada à API do modelo interrompida.", e);
        }
    }

    /**
     * POST com {@code "stream": true}: entrega cada evento SSE em {@code onEvent} e devolve o
     * objeto {@code response} do evento {@code response.completed}.
     *
     * @param onOpen recebe o corpo aberto; fechá-lo de outra thread interrompe a leitura (Parar)
     */
    public JsonNode stream(
            String path,
            JsonNode body,
            Consumer<JsonNode> onEvent,
            Consumer<InputStream> onOpen
    ) {
        if (!this.settings.hasApiKey()) {
            throw new OpenAiNotConfiguredException();
        }

        HttpRequest request = HttpRequest
                .newBuilder(URI.create(this.settings.baseUrl() + path))
                .timeout(this.settings.timeout())
                .header("Authorization", "Bearer " + this.settings.apiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(this.write(body)))
                .build();

        try {
            HttpResponse<InputStream> response = this.http.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() / 100 != 2) {
                try (InputStream error = response.body()) {
                    throw new OpenAiApiException(response.statusCode(), path, new String(error.readAllBytes(), StandardCharsets.UTF_8));
                }
            }

            onOpen.accept(response.body());

            return this.read(path, response.body(), onEvent);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha de comunicação com " + this.settings.baseUrl() + path + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chamada à API do modelo interrompida.", e);
        }
    }

    private JsonNode read(
            String path,
            InputStream body,
            Consumer<JsonNode> onEvent
    ) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }

                String data = line.substring(5).strip();

                if (data.isEmpty() || "[DONE]".equals(data)) {
                    continue;
                }

                JsonNode event = this.json.readTree(data);
                String type = event.path("type").asText();

                onEvent.accept(event);

                if ("response.completed".equals(type) || "response.incomplete".equals(type)) {
                    return event.path("response");
                }

                if ("response.failed".equals(type) || "error".equals(type)) {
                    throw new OpenAiApiException(500, path, data);
                }
            }
        }

        throw new UncheckedIOException(new IOException("O stream do modelo terminou sem response.completed"));
    }

    private String write(JsonNode body) {
        try {
            return this.json.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível serializar a requisição: " + e.getMessage(), e);
        }
    }
}
