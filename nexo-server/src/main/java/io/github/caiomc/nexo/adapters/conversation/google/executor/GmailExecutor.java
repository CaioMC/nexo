package io.github.caiomc.nexo.adapters.conversation.google.executor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.caiomc.nexo.adapters.conversation.google.GoogleCall;
import io.github.caiomc.nexo.adapters.conversation.google.GoogleRequest;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

/**
 * Gmail: busca, leitura, rascunhos e etiquetas.
 */
public class GmailExecutor implements GoogleContextExecutor {

    private static final int MAX_BODY = 40_000;

    @Override
    public ContextKey context() {
        return ContextKey.of("gmail");
    }

    @Override
    public JsonNode execute(GoogleCall call) {
        String api = call.endpoints().gmail();

        return switch (call.tool().value()) {
            case "google_gmail_search" -> this.search(call, api);
            case "google_gmail_get_message" -> this.message(call, api);
            case "google_gmail_list_drafts" -> call.get(api + "/drafts", Map.of("maxResults", call.integer("maxResults", 10)));
            case "google_gmail_list_labels" -> call.get(api + "/labels", Map.of());
            case "google_gmail_create_draft" -> this.draft(call, api);
            case "google_gmail_modify_message" -> this.modify(call, api + "/messages/" + Params.path(call.text("messageId")) + "/modify");
            case "google_gmail_modify_thread" -> this.modify(call, api + "/threads/" + Params.path(call.text("threadId")) + "/modify");
            default -> throw new IllegalArgumentException("Ferramenta fora do Gmail: " + call.tool());
        };
    }

    private JsonNode search(
            GoogleCall call,
            String api
    ) {
        JsonNode found = call.get(api + "/messages", Map.of("q", call.text("query"), "maxResults", call.integer("maxResults", 10)));
        ObjectNode result = call.json().createObjectNode();
        ArrayNode messages = result.putArray("messages");

        for (JsonNode item : found.path("messages")) {
            JsonNode message = call.get(
                    api + "/messages/" + Params.path(item.path("id").asText()),
                    Map.of("format", "metadata", "metadataHeaders", List.of("From", "To", "Subject", "Date"))
            );

            messages.addObject()
                    .put("id", message.path("id").asText())
                    .put("threadId", message.path("threadId").asText())
                    .put("snippet", message.path("snippet").asText())
                    .set("headers", message.path("payload").path("headers"));
        }

        return result;
    }

    private JsonNode message(
            GoogleCall call,
            String api
    ) {
        JsonNode message = call.get(api + "/messages/" + Params.path(call.text("messageId")), Map.of("format", "full"));
        String body = this.body(message.path("payload"));
        ObjectNode result = call.json().createObjectNode()
                .put("id", message.path("id").asText())
                .put("threadId", message.path("threadId").asText())
                .put("snippet", message.path("snippet").asText())
                .put("body", body.length() > MAX_BODY ? body.substring(0, MAX_BODY) : body);

        result.set("headers", message.path("payload").path("headers"));

        return result;
    }

    private JsonNode draft(
            GoogleCall call,
            String api
    ) {
        List<String> lines = new ArrayList<>();

        lines.add("To: " + String.join(", ", call.strings("to")));

        if (!call.strings("cc").isEmpty()) {
            lines.add("Cc: " + String.join(", ", call.strings("cc")));
        }

        if (!call.strings("bcc").isEmpty()) {
            lines.add("Bcc: " + String.join(", ", call.strings("bcc")));
        }

        lines.add("Subject: " + call.text("subject"));
        lines.add("MIME-Version: 1.0");
        lines.add("Content-Type: text/plain; charset=UTF-8");
        lines.add("");
        lines.add(call.text("body"));

        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(String.join("\r\n", lines).getBytes(StandardCharsets.UTF_8));

        return call.send(GoogleRequest.post(api + "/drafts", Map.of("message", Map.of("raw", raw))));
    }

    private JsonNode modify(
            GoogleCall call,
            String url
    ) {
        return call.send(GoogleRequest.post(url, Map.of("addLabelIds", call.strings("addLabelIds"), "removeLabelIds", call.strings("removeLabelIds"))));
    }

    /**
     * Texto do e-mail: a parte {@code text/plain}, procurada recursivamente.
     */
    private String body(JsonNode payload) {
        if ("text/plain".equals(payload.path("mimeType").asText()) && payload.path("body").has("data")) {
            return this.decode(payload.path("body").path("data").asText());
        }

        for (JsonNode part : payload.path("parts")) {
            String text = this.body(part);

            if (!text.isEmpty()) {
                return text;
            }
        }

        return payload.path("body").has("data") ? this.decode(payload.path("body").path("data").asText()) : "";
    }

    private String decode(String data) {
        return new String(Base64.getUrlDecoder().decode(data.replace('+', '-').replace('/', '_')), StandardCharsets.UTF_8);
    }
}
