package io.github.caiomc.nexo.adapters.conversation.google.executor;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.caiomc.nexo.adapters.conversation.google.GoogleCall;
import io.github.caiomc.nexo.adapters.conversation.google.GoogleRequest;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Calendar: agendas e eventos.
 */
public class CalendarExecutor implements GoogleContextExecutor {

    private static final String PRIMARY = "primary";

    @Override
    public ContextKey context() {
        return ContextKey.of("calendar");
    }

    @Override
    public JsonNode execute(GoogleCall call) {
        String api = call.endpoints().calendar();

        return switch (call.tool().value()) {
            case "google_calendar_list_calendars" -> call.get(api + "/users/me/calendarList", Map.of("fields", "items(id,summary,primary,timeZone,accessRole)"));
            case "google_calendar_list_events" -> call.get(api + "/calendars/primary/events", Params.of(
                    "timeMin", call.optionalText("timeMin"),
                    "timeMax", call.optionalText("timeMax"),
                    "q", call.optionalText("query"),
                    "maxResults", call.integer("maxResults", 10),
                    "singleEvents", true,
                    "orderBy", "startTime"
            ));
            case "google_calendar_get_event" -> call.get(this.event(call, api), Map.of());
            case "google_calendar_create_event" -> this.create(call, api);
            case "google_calendar_update_event" -> this.update(call, api);
            case "google_calendar_respond_to_event" -> call.send(GoogleRequest.patch(this.event(call, api), Map.of("sendUpdates", "none"), Map.of("attendees", List.of(Map.of("self", true, "responseStatus", call.text("responseStatus"))))));
            case "google_calendar_delete_event" -> call.send(GoogleRequest.delete(this.event(call, api), Map.of("sendUpdates", "all")));
            default -> throw new IllegalArgumentException("Ferramenta fora do Calendar: " + call.tool());
        };
    }

    private JsonNode create(
            GoogleCall call,
            String api
    ) {
        Map<String, Object> body = Params.of(
                "summary", call.text("summary"),
                "start", Map.of("dateTime", call.text("start")),
                "end", Map.of("dateTime", call.text("end"))
        );

        if (nonNull(call.optionalText("description"))) {
            body.put("description", call.optionalText("description"));
        }

        if (!call.strings("attendees").isEmpty()) {
            body.put("attendees", call.strings("attendees").stream().map(email -> Map.of("email", email)).toList());
        }

        return call.send(GoogleRequest.post(api + "/calendars/" + this.calendar(call) + "/events", Map.of("sendUpdates", "all"), body));
    }

    /**
     * Só os campos informados mudam; início e fim mantêm o fuso do evento atual.
     */
    private JsonNode update(
            GoogleCall call,
            String api
    ) {
        String url = this.event(call, api);
        JsonNode current = call.get(url, Map.of());
        Map<String, Object> body = Params.of();

        if (nonNull(call.optionalText("summary"))) {
            body.put("summary", call.optionalText("summary"));
        }

        if (nonNull(call.optionalText("description"))) {
            body.put("description", call.optionalText("description"));
        }

        if (nonNull(call.optionalText("start"))) {
            body.put("start", this.time(current.path("start"), call.optionalText("start")));
        }

        if (nonNull(call.optionalText("end"))) {
            body.put("end", this.time(current.path("end"), call.optionalText("end")));
        }

        return call.send(GoogleRequest.patch(url, Map.of("sendUpdates", "all"), body));
    }

    private Map<String, Object> time(
            JsonNode current,
            String dateTime
    ) {
        Map<String, Object> time = Params.of("dateTime", dateTime);

        if (current.has("timeZone")) {
            time.put("timeZone", current.path("timeZone").asText());
        }

        return time;
    }

    private String event(
            GoogleCall call,
            String api
    ) {
        return api + "/calendars/" + this.calendar(call) + "/events/" + Params.path(call.text("eventId"));
    }

    private String calendar(GoogleCall call) {
        String calendar = call.optionalText("calendarId");

        return Params.path(isNull(calendar) ? PRIMARY : calendar);
    }
}
