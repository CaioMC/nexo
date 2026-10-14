package io.github.caiomc.nexo.adapters.conversation.google;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static java.util.Objects.isNull;

/**
 * HTTP para as APIs REST do Google com o access token do usuário. 429 e 5xx: até 3 novas
 * tentativas com espera de 250/500/1000 ms + jitter (como o {@code ToolRegistry} do google-tools).
 */
public class GoogleApiClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleApiClient.class);

    private static final long[] BACKOFF_MS = {250, 500, 1000};
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final ObjectMapper json;
    private final HttpClient http;

    public GoogleApiClient(ObjectMapper json) {
        this.json = json;
        this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    public JsonNode call(
            GoogleRequest request,
            String token
    ) {
        for (int attempt = 0; ; attempt++) {
            HttpResponse<String> response = this.send(request, token);
            int status = response.statusCode();

            if (status / 100 == 2) {
                return this.read(response.body());
            }

            if ((status == 429 || status >= 500) && attempt < BACKOFF_MS.length) {
                log.debug("Google respondeu {} em {}: nova tentativa", status, request.url());
                this.sleep(BACKOFF_MS[attempt] + ThreadLocalRandom.current().nextLong(100));

                continue;
            }

            JsonNode error = this.read(response.body()).path("error");

            throw new GoogleApiException(
                    status,
                    this.reason(error),
                    error.path("message").asText("A API do Google respondeu " + status)
            );
        }
    }

    private HttpResponse<String> send(
            GoogleRequest request,
            String token
    ) {
        HttpRequest.Builder builder = HttpRequest
                .newBuilder(URI.create(request.url() + this.query(request.query())))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json");

        if (isNull(request.body())) {
            builder.method(request.method(), HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", isNull(request.contentType()) ? "application/json" : request.contentType());
            builder.method(request.method(), HttpRequest.BodyPublishers.ofByteArray(this.bytes(request.body())));
        }

        try {
            return this.http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new GoogleApiException(502, "unreachable", "A API do Google não respondeu: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GoogleApiException(499, "interrupted", "Chamada ao Google interrompida");
        }
    }

    private String query(Map<String, Object> query) {
        List<String> pairs = new ArrayList<>();

        query.forEach((key, value) -> {
            if (isNull(value) || "".equals(value)) {
                return;
            }

            if (value instanceof Collection<?> values) {
                values.forEach(item -> pairs.add(this.encode(key) + "=" + this.encode(String.valueOf(item))));
            } else {
                pairs.add(this.encode(key) + "=" + this.encode(String.valueOf(value)));
            }
        });

        return pairs.isEmpty() ? "" : "?" + String.join("&", pairs);
    }

    private byte[] bytes(Object body) {
        if (body instanceof byte[] raw) {
            return raw;
        }

        try {
            return this.json.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Corpo da chamada ao Google não serializável: " + e.getMessage(), e);
        }
    }

    private JsonNode read(String body) {
        try {
            return isNull(body) || body.isBlank() ? this.json.createObjectNode() : this.json.readTree(body);
        } catch (JsonProcessingException e) {
            return this.json.createObjectNode().put("raw", body);
        }
    }

    private String reason(JsonNode error) {
        JsonNode first = error.path("errors").path(0).path("reason");

        if (!first.isMissingNode()) {
            return first.asText();
        }

        for (JsonNode detail : error.path("details")) {
            if (detail.has("reason")) {
                return detail.path("reason").asText();
            }
        }

        return error.path("status").asText("");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GoogleApiException(499, "interrupted", "Chamada ao Google interrompida");
        }
    }
}
