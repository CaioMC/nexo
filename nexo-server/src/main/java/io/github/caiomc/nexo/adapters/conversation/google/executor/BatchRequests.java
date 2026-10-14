package io.github.caiomc.nexo.adapters.conversation.google.executor;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import io.github.caiomc.nexo.adapters.conversation.google.GoogleCall;

/**
 * {@code requestsJson} dos {@code *_batch_update}: precisa ser um array JSON de objetos.
 */
final class BatchRequests {

    private static final TypeReference<List<Map<String, Object>>> REQUESTS = new TypeReference<>() {
    };

    private BatchRequests() {
    }

    static List<Map<String, Object>> parse(
            GoogleCall call,
            String argument
    ) {
        try {
            List<Map<String, Object>> requests = call.json().readValue(call.text(argument), REQUESTS);

            if (requests.isEmpty()) {
                throw new IllegalArgumentException(argument + " precisa ter ao menos um pedido");
            }

            return requests;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(argument + " precisa ser um array JSON de objetos: " + e.getOriginalMessage());
        }
    }
}
