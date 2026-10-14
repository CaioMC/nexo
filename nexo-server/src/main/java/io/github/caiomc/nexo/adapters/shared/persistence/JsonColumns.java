package io.github.caiomc.nexo.adapters.shared.persistence;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static java.util.Objects.isNull;

/**
 * Colunas {@code jsonb}: o SQL grava com {@code CAST(:x AS jsonb)} e lê com {@code x::text}.
 */
@Component
public class JsonColumns {

    private static final TypeReference<Map<String, Object>> OBJECT = new TypeReference<>() {
    };

    private final ObjectMapper json;

    public JsonColumns(ObjectMapper json) {
        this.json = json;
    }

    public String write(Object value) {
        try {
            return this.json.writeValueAsString(isNull(value) ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Valor não serializável em JSON: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> read(String text) {
        try {
            return isNull(text) ? Map.of() : this.json.readValue(text, OBJECT);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON inválido no banco: " + e.getMessage(), e);
        }
    }
}
