package io.github.caiomc.nexo.adapters.conversation.google.executor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.github.caiomc.nexo.adapters.conversation.google.GoogleCall;
import io.github.caiomc.nexo.adapters.conversation.google.GoogleRequest;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

import static java.util.Objects.nonNull;

/**
 * Drive: busca, metadados, permissões e arquivos de texto.
 */
public class DriveExecutor implements GoogleContextExecutor {

    private static final String FILE_FIELDS = "id,name,mimeType,webViewLink";

    @Override
    public ContextKey context() {
        return ContextKey.of("drive");
    }

    @Override
    public JsonNode execute(GoogleCall call) {
        String api = call.endpoints().drive();

        return switch (call.tool().value()) {
            case "google_drive_search" -> this.search(call, api);
            case "google_drive_get_metadata" -> call.get(api + "/files/" + this.fileId(call), Map.of("fields", "id,name,mimeType,modifiedTime,size,webViewLink,owners(displayName,emailAddress)"));
            case "google_drive_list_recent" -> call.get(api + "/files", Map.of("q", "trashed = false", "pageSize", call.integer("maxResults", 10), "fields", "files(id,name,mimeType,modifiedTime,webViewLink)", "orderBy", "modifiedTime desc"));
            case "google_drive_get_permissions" -> call.get(api + "/files/" + this.fileId(call) + "/permissions", Map.of("fields", "permissions(id,type,role,emailAddress,displayName,allowFileDiscovery)"));
            case "google_drive_create_file" -> this.create(call);
            case "google_drive_update_content" -> call.send(new GoogleRequest(
                    "PATCH",
                    call.endpoints().driveUpload() + "/files/" + this.fileId(call),
                    Map.of("uploadType", "media", "fields", FILE_FIELDS),
                    call.text("content").getBytes(StandardCharsets.UTF_8),
                    "text/plain; charset=UTF-8"
            ));
            case "google_drive_rename_file" -> call.send(GoogleRequest.patch(api + "/files/" + this.fileId(call), Map.of("fields", FILE_FIELDS), Map.of("name", call.text("name"))));
            case "google_drive_copy_file" -> call.send(GoogleRequest.post(api + "/files/" + this.fileId(call) + "/copy", Map.of("fields", FILE_FIELDS), this.metadata(call.text("name"), call.optionalText("parentId"), null)));
            case "google_drive_trash_file" -> call.send(GoogleRequest.patch(api + "/files/" + this.fileId(call), Map.of("fields", "id,name,trashed"), Map.of("trashed", true)));
            case "google_drive_delete_file" -> call.send(GoogleRequest.delete(api + "/files/" + this.fileId(call), Map.of()));
            default -> throw new IllegalArgumentException("Ferramenta fora do Drive: " + call.tool());
        };
    }

    private JsonNode search(
            GoogleCall call,
            String api
    ) {
        String escaped = call.text("query").replace("\\", "\\\\").replace("'", "\\'");
        String query = "trashed = false and (name contains '" + escaped + "' or fullText contains '" + escaped + "')";

        return call.get(api + "/files", Map.of("q", query, "pageSize", call.integer("maxResults", 10), "fields", "files(id,name,mimeType,modifiedTime,size,webViewLink),nextPageToken", "orderBy", "modifiedTime desc"));
    }

    /**
     * Upload multipart: metadados em JSON + conteúdo em texto.
     */
    private JsonNode create(GoogleCall call) {
        String boundary = "nexo_" + UUID.randomUUID();
        String metadata = this.json(call, this.metadata(call.text("name"), call.optionalText("parentId"), "text/plain"));
        String contentType = "multipart/related; boundary=" + boundary;
        String body = "--" + boundary + "\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n" + metadata + "\r\n--" + boundary + "\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n" + call.text("content") + "\r\n--" + boundary + "--";

        return call.send(new GoogleRequest(
                "POST",
                call.endpoints().driveUpload() + "/files",
                Map.of("uploadType", "multipart", "fields", FILE_FIELDS),
                body.getBytes(StandardCharsets.UTF_8),
                contentType
        ));
    }

    private Map<String, Object> metadata(
            String name,
            String parentId,
            String mimeType
    ) {
        Map<String, Object> metadata = Params.of("name", name);

        if (nonNull(mimeType)) {
            metadata.put("mimeType", mimeType);
        }

        if (nonNull(parentId)) {
            metadata.put("parents", List.of(parentId));
        }

        return metadata;
    }

    private String json(
            GoogleCall call,
            Object value
    ) {
        try {
            return call.json().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Metadados inválidos: " + e.getMessage(), e);
        }
    }

    private String fileId(GoogleCall call) {
        return Params.path(call.text("fileId"));
    }
}
