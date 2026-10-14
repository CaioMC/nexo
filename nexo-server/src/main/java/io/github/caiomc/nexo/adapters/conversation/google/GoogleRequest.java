package io.github.caiomc.nexo.adapters.conversation.google;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * Uma chamada REST ao Google.
 *
 * @param body        JSON (Map/List) ou {@code byte[]} já montado; nulo = sem corpo
 * @param contentType nulo = {@code application/json}
 */
public record GoogleRequest(
        String method,
        String url,
        Map<String, Object> query,
        Object body,
        String contentType
) {

    public GoogleRequest {
        query = isNull(query) ? Map.of() : new LinkedHashMap<>(query);
    }

    public static GoogleRequest get(
            String url,
            Map<String, Object> query
    ) {
        return new GoogleRequest(
                "GET",
                url,
                query,
                null,
                null
        );
    }

    public static GoogleRequest post(
            String url,
            Map<String, Object> query,
            Object body
    ) {
        return new GoogleRequest(
                "POST",
                url,
                query,
                body,
                null
        );
    }

    public static GoogleRequest post(
            String url,
            Object body
    ) {
        return post(url, Map.of(), body);
    }

    public static GoogleRequest put(
            String url,
            Map<String, Object> query,
            Object body
    ) {
        return new GoogleRequest(
                "PUT",
                url,
                query,
                body,
                null
        );
    }

    public static GoogleRequest patch(
            String url,
            Map<String, Object> query,
            Object body
    ) {
        return new GoogleRequest(
                "PATCH",
                url,
                query,
                body,
                null
        );
    }

    public static GoogleRequest delete(
            String url,
            Map<String, Object> query
    ) {
        return new GoogleRequest(
                "DELETE",
                url,
                query,
                null,
                null
        );
    }
}
