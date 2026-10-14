package io.github.caiomc.nexo.adapters.conversation.google.executor;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.caiomc.nexo.adapters.conversation.google.GoogleCall;
import io.github.caiomc.nexo.adapters.conversation.google.GoogleRequest;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

/**
 * Chat: espaços, mensagens e membros.
 */
public class ChatExecutor implements GoogleContextExecutor {

    @Override
    public ContextKey context() {
        return ContextKey.of("chat");
    }

    @Override
    public JsonNode execute(GoogleCall call) {
        String api = call.endpoints().chat();

        return switch (call.tool().value()) {
            case "google_chat_list_spaces" -> call.get(api + "/spaces", Map.of("pageSize", call.integer("pageSize", 20)));
            case "google_chat_list_messages" -> call.get(api + "/" + this.space(call) + "/messages", Map.of("pageSize", call.integer("pageSize", 20), "orderBy", "createTime desc"));
            case "google_chat_list_memberships" -> call.get(api + "/" + this.space(call) + "/memberships", Map.of("pageSize", call.integer("pageSize", 20)));
            case "google_chat_send_message" -> call.send(GoogleRequest.post(api + "/" + this.space(call) + "/messages", Map.of("text", call.text("text"))));
            case "google_chat_mark_read" -> call.send(GoogleRequest.patch(api + "/users/me/" + this.space(call) + "/spaceReadState", Map.of("updateMask", "lastReadTime"), Map.of("lastReadTime", Instant.now().toString())));
            case "google_chat_mark_unread" -> call.send(GoogleRequest.patch(api + "/users/me/" + this.space(call) + "/spaceReadState", Map.of("updateMask", "lastReadTime"), Map.of("lastReadTime", "1970-01-01T00:00:00Z")));
            default -> throw new IllegalArgumentException("Ferramenta fora do Chat: " + call.tool());
        };
    }

    /**
     * {@code spaces/AAAA}: o nome do recurso é o caminho; só confere o formato.
     */
    private String space(GoogleCall call) {
        String space = call.text("space");

        if (!space.matches("^spaces/[A-Za-z0-9_-]+$")) {
            throw new IllegalArgumentException("space precisa ser o nome do recurso, como spaces/AAAAxyz");
        }

        return space;
    }
}
