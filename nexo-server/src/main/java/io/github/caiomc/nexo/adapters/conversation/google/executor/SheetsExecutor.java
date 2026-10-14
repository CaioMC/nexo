package io.github.caiomc.nexo.adapters.conversation.google.executor;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.caiomc.nexo.adapters.conversation.google.GoogleCall;
import io.github.caiomc.nexo.adapters.conversation.google.GoogleRequest;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

/**
 * Sheets: ler, criar e alterar planilhas.
 */
public class SheetsExecutor implements GoogleContextExecutor {

    @Override
    public ContextKey context() {
        return ContextKey.of("sheets");
    }

    @Override
    public JsonNode execute(GoogleCall call) {
        String api = call.endpoints().sheets();

        return switch (call.tool().value()) {
            case "google_sheets_get_spreadsheet" -> call.get(api + "/" + this.id(call), Map.of("ranges", call.strings("ranges"), "includeGridData", !call.strings("ranges").isEmpty(), "fields", "spreadsheetId,properties.title,sheets.properties,sheets.data.rowData.values.formattedValue"));
            case "google_sheets_get_values" -> call.get(api + "/" + this.id(call) + "/values/" + this.range(call), Map.of());
            case "google_sheets_create" -> call.send(GoogleRequest.post(api, Map.of("properties", Map.of("title", call.text("title")))));
            case "google_sheets_update_values" -> call.send(GoogleRequest.put(
                    api + "/" + this.id(call) + "/values/" + this.range(call),
                    Map.of("valueInputOption", "USER_ENTERED"),
                    Map.of("range", call.text("range"), "majorDimension", "ROWS", "values", call.list("values"))
            ));
            case "google_sheets_clear_values" -> call.send(GoogleRequest.post(api + "/" + this.id(call) + "/values/" + this.range(call) + ":clear", Map.of()));
            case "google_sheets_batch_update" -> call.send(GoogleRequest.post(api + "/" + this.id(call) + ":batchUpdate", Map.of("requests", BatchRequests.parse(call, "requestsJson"))));
            default -> throw new IllegalArgumentException("Ferramenta fora do Sheets: " + call.tool());
        };
    }

    private String id(GoogleCall call) {
        return Params.path(call.text("spreadsheetId"));
    }

    private String range(GoogleCall call) {
        return Params.path(call.text("range"));
    }
}
