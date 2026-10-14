package io.github.caiomc.nexo.adapters.conversation.google;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;

import static java.util.Objects.isNull;

/**
 * Uma execução: os argumentos que o modelo mandou (já validados pelo gatekeeper), o token e o
 * cliente HTTP. Os executores leem argumentos por aqui; texto vazio conta como "não informado".
 */
public class GoogleCall {

    private final ToolName tool;
    private final Map<String, Object> arguments;
    private final GoogleApiClient client;
    private final GoogleApiEndpoints endpoints;
    private final ObjectMapper json;
    private final String token;

    public GoogleCall(
            ToolName tool,
            Map<String, Object> arguments,
            GoogleApiClient client,
            GoogleApiEndpoints endpoints,
            ObjectMapper json,
            String token
    ) {
        this.tool = tool;
        this.arguments = arguments;
        this.client = client;
        this.endpoints = endpoints;
        this.json = json;
        this.token = token;
    }

    public ToolName tool() {
        return this.tool;
    }

    public GoogleApiEndpoints endpoints() {
        return this.endpoints;
    }

    public ObjectMapper json() {
        return this.json;
    }

    public JsonNode send(GoogleRequest request) {
        return this.client.call(request, this.token);
    }

    public JsonNode get(
            String url,
            Map<String, Object> query
    ) {
        return this.send(GoogleRequest.get(url, query));
    }

    /**
     * Argumento obrigatório de texto.
     */
    public String text(String name) {
        String value = this.optionalText(name);

        if (isNull(value)) {
            throw new IllegalArgumentException("Informe " + name);
        }

        return value;
    }

    /**
     * @return nulo se ausente ou vazio
     */
    public String optionalText(String name) {
        Object value = this.arguments.get(name);

        if (isNull(value)) {
            return null;
        }

        String text = String.valueOf(value);

        return text.isBlank() ? null : text;
    }

    public int integer(
            String name,
            int fallback
    ) {
        return this.arguments.get(name) instanceof Number number ? number.intValue() : fallback;
    }

    public boolean bool(
            String name,
            boolean fallback
    ) {
        return this.arguments.get(name) instanceof Boolean value ? value : fallback;
    }

    public List<?> list(String name) {
        return this.arguments.get(name) instanceof List<?> values ? values : List.of();
    }

    public List<String> strings(String name) {
        return this.list(name).stream().map(String::valueOf).filter(value -> !value.isBlank()).toList();
    }

    public Object raw(String name) {
        return this.arguments.get(name);
    }
}
