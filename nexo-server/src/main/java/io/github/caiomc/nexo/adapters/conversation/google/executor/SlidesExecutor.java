package io.github.caiomc.nexo.adapters.conversation.google.executor;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.caiomc.nexo.adapters.conversation.google.GoogleCall;
import io.github.caiomc.nexo.adapters.conversation.google.GoogleRequest;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

/**
 * Slides: ler, criar e editar apresentações.
 */
public class SlidesExecutor implements GoogleContextExecutor {

    @Override
    public ContextKey context() {
        return ContextKey.of("slides");
    }

    @Override
    public JsonNode execute(GoogleCall call) {
        String api = call.endpoints().slides();

        return switch (call.tool().value()) {
            case "google_slides_read" -> this.read(call, api);
            case "google_slides_create" -> call.send(GoogleRequest.post(api, Map.of("title", call.text("title"))));
            case "google_slides_replace_text" -> call.send(GoogleRequest.post(
                    api + "/" + this.id(call) + ":batchUpdate",
                    Map.of("requests", List.of(Map.of("replaceAllText", Map.of("containsText", Map.of("text", call.text("find"), "matchCase", call.bool("matchCase", false)), "replaceText", call.text("replace")))))
            ));
            case "google_slides_batch_update" -> call.send(GoogleRequest.post(api + "/" + this.id(call) + ":batchUpdate", Map.of("requests", BatchRequests.parse(call, "requestsJson"))));
            default -> throw new IllegalArgumentException("Ferramenta fora do Slides: " + call.tool());
        };
    }

    private JsonNode read(
            GoogleCall call,
            String api
    ) {
        JsonNode presentation = call.get(api + "/" + this.id(call), Map.of());

        return call.json().createObjectNode()
                .put("presentationId", presentation.path("presentationId").asText())
                .put("title", presentation.path("title").asText())
                .put("slides", presentation.path("slides").size())
                .put("text", DocumentText.of(presentation.path("slides")));
    }

    private String id(GoogleCall call) {
        return Params.path(call.text("presentationId"));
    }
}
