package io.github.caiomc.nexo.adapters.retrieval.openai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.adapters.shared.openai.OpenAiHttpClient;
import io.github.caiomc.nexo.adapters.shared.openai.OpenAiSettings;
import io.github.caiomc.nexo.core.retrieval.port.embedding.EmbeddingPort;
import io.github.caiomc.nexo.core.retrieval.port.embedding.command.EmbedTextsCommand;
import io.github.caiomc.nexo.core.retrieval.port.embedding.data.EmbeddingData;


/**
 * {@link EmbeddingPort} sobre {@code POST /embeddings} (OpenAI ou proxy compatível), em lotes de até
 * {@value #BATCH_SIZE} textos. A resposta é reordenada pelo {@code index} de cada item.
 */
@Component
public class OpenAiEmbeddingAdapter implements EmbeddingPort {

    private static final int BATCH_SIZE = 100;

    private final OpenAiHttpClient http;
    private final ObjectMapper json;
    private final String model;

    public OpenAiEmbeddingAdapter(
            OpenAiHttpClient http,
            ObjectMapper json,
            OpenAiSettings settings
    ) {
        this.http = http;
        this.json = json;
        this.model = settings.embeddingModel();
    }

    @Override
    public List<EmbeddingData> embed(EmbedTextsCommand command) {
        List<String> texts = command.texts();
        List<EmbeddingData> embeddings = new ArrayList<>();

        for (int start = 0; start < texts.size(); start += BATCH_SIZE) {
            embeddings.addAll(this.embedBatch(texts.subList(start, Math.min(start + BATCH_SIZE, texts.size()))));
        }

        return embeddings;
    }

    @Override
    public String model() {
        return this.model;
    }

    @Override
    public boolean available() {
        return this.http.configured();
    }

    private List<EmbeddingData> embedBatch(List<String> batch) {
        ObjectNode body = this.json.createObjectNode().put("model", this.model);
        ArrayNode input = body.putArray("input");

        batch.forEach(input::add);

        JsonNode response = this.http.post("/embeddings", body);
        EmbeddingData[] ordered = new EmbeddingData[batch.size()];

        for (JsonNode item : response.path("data")) {
            ordered[item.path("index").asInt()] = this.toData(item.path("embedding"));
        }

        if (Arrays.stream(ordered).anyMatch(Objects::isNull)) {
            throw new IllegalStateException("A API de embeddings devolveu menos vetores que textos enviados (" + batch.size() + ")");
        }

        return List.of(ordered);
    }

    private EmbeddingData toData(JsonNode vector) {
        double[] values = new double[vector.size()];

        for (int i = 0; i < values.length; i++) {
            values[i] = vector.get(i).asDouble();
        }

        return new EmbeddingData(values);
    }
}
