package io.github.caiomc.nexo.adapters.conversation.google.executor;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.caiomc.nexo.adapters.conversation.google.GoogleCall;
import io.github.caiomc.nexo.adapters.conversation.google.GoogleRequest;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

import static java.util.Objects.nonNull;

/**
 * Contatos (People API).
 */
public class PeopleExecutor implements GoogleContextExecutor {

    private static final String FIELDS = "names,emailAddresses,phoneNumbers";

    @Override
    public ContextKey context() {
        return ContextKey.of("people");
    }

    @Override
    public JsonNode execute(GoogleCall call) {
        String api = call.endpoints().people();

        return switch (call.tool().value()) {
            case "google_people_get_profile" -> call.get(api + "/people/me", Map.of("personFields", "names,emailAddresses,photos,organizations"));
            case "google_people_search_contacts" -> call.get(api + "/people:searchContacts", Map.of("query", call.text("query"), "readMask", "names,emailAddresses,phoneNumbers,organizations", "pageSize", call.integer("pageSize", 10)));
            case "google_people_create_contact" -> call.send(GoogleRequest.post(api + "/people:createContact", Map.of("personFields", FIELDS), this.contact(call)));
            case "google_people_update_contact" -> this.update(call, api);
            case "google_people_delete_contact" -> call.send(GoogleRequest.delete(api + "/" + this.resource(call) + ":deleteContact", Map.of()));
            default -> throw new IllegalArgumentException("Ferramenta fora de Contatos: " + call.tool());
        };
    }

    private JsonNode update(
            GoogleCall call,
            String api
    ) {
        Map<String, Object> body = this.contact(call);

        body.put("etag", call.text("etag"));

        return call.send(GoogleRequest.patch(api + "/" + this.resource(call) + ":updateContact", Map.of("updatePersonFields", FIELDS, "personFields", FIELDS), body));
    }

    private Map<String, Object> contact(GoogleCall call) {
        Map<String, Object> body = Params.of();
        Map<String, Object> name = Params.of();

        if (nonNull(call.optionalText("givenName"))) {
            name.put("givenName", call.optionalText("givenName"));
        }

        if (nonNull(call.optionalText("familyName"))) {
            name.put("familyName", call.optionalText("familyName"));
        }

        if (!name.isEmpty()) {
            body.put("names", List.of(name));
        }

        if (nonNull(call.optionalText("email"))) {
            body.put("emailAddresses", List.of(Map.of("value", call.optionalText("email"))));
        }

        if (nonNull(call.optionalText("phone"))) {
            body.put("phoneNumbers", List.of(Map.of("value", call.optionalText("phone"))));
        }

        return body;
    }

    /**
     * {@code people/c123}: o nome do recurso é o caminho; só confere o formato.
     */
    private String resource(GoogleCall call) {
        String resource = call.text("resourceName");

        if (!resource.matches("^people/[A-Za-z0-9_-]+$")) {
            throw new IllegalArgumentException("resourceName precisa ser como people/c123");
        }

        return resource;
    }
}
