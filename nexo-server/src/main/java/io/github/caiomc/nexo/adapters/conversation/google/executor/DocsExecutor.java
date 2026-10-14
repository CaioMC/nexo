package io.github.caiomc.nexo.adapters.conversation.google.executor;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.caiomc.nexo.adapters.conversation.google.GoogleCall;
import io.github.caiomc.nexo.adapters.conversation.google.GoogleRequest;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

import static java.util.Objects.nonNull;

/**
 * Docs: ler, criar e editar documentos.
 */
public class DocsExecutor implements GoogleContextExecutor {

    @Override
    public ContextKey context() {
        return ContextKey.of("docs");
    }

    @Override
    public JsonNode execute(GoogleCall call) {
        String api = call.endpoints().docs();

        return switch (call.tool().value()) {
            case "google_docs_read" -> this.read(call, api);
            case "google_docs_create" -> this.create(call, api);
            case "google_docs_replace_text" -> this.batch(call, api, List.of(Map.of("replaceAllText", Map.of("containsText", Map.of("text", call.text("find"), "matchCase", call.bool("matchCase", false)), "replaceText", call.text("replace")))));
            case "google_docs_append_text" -> this.append(call, api);
            case "google_docs_batch_update" -> this.batch(call, api, BatchRequests.parse(call, "requestsJson"));
            default -> throw new IllegalArgumentException("Ferramenta fora do Docs: " + call.tool());
        };
    }

    private JsonNode read(
            GoogleCall call,
            String api
    ) {
        JsonNode document = call.get(api + "/" + this.documentId(call), Map.of());

        return call.json().createObjectNode()
                .put("documentId", document.path("documentId").asText())
                .put("title", document.path("title").asText())
                .put("text", DocumentText.of(document.path("body")));
    }

    private JsonNode create(
            GoogleCall call,
            String api
    ) {
        JsonNode document = call.send(GoogleRequest.post(api, Map.of("title", call.text("title"))));
        String initial = call.optionalText("initialText");

        if (nonNull(initial)) {
            call.send(GoogleRequest.post(
                    api + "/" + Params.path(document.path("documentId").asText()) + ":batchUpdate",
                    Map.of("requests", List.of(Map.of("insertText", Map.of("location", Map.of("index", 1), "text", initial))))
            ));
        }

        return call.json().createObjectNode()
                .put("documentId", document.path("documentId").asText())
                .put("title", document.path("title").asText());
    }

    /**
     * Insere no fim: o índice final do corpo menos 1 (o último {@code \n} é do documento).
     */
    private JsonNode append(
            GoogleCall call,
            String api
    ) {
        JsonNode content = call.get(api + "/" + this.documentId(call), Map.of()).path("body").path("content");
        int end = Math.max(1, content.path(content.size() - 1).path("endIndex").asInt(2) - 1);

        return this.batch(call, api, List.of(Map.of("insertText", Map.of("location", Map.of("index", end), "text", call.text("text")))));
    }

    private JsonNode batch(
            GoogleCall call,
            String api,
            List<Map<String, Object>> requests
    ) {
        return call.send(GoogleRequest.post(api + "/" + this.documentId(call) + ":batchUpdate", Map.of("requests", requests)));
    }

    private String documentId(GoogleCall call) {
        return Params.path(call.text("documentId"));
    }
}
