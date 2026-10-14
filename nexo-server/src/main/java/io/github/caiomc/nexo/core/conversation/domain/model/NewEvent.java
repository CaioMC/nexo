package io.github.caiomc.nexo.core.conversation.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import io.github.caiomc.nexo.core.conversation.domain.enums.EventType;

import static java.util.Objects.isNull;

/**
 * Evento ainda sem número: o {@code ConversationEventHubService} grava e ele ganha {@code seq}. O payload
 * só tem tipos que viram JSON (texto, número, booleano, lista, mapa).
 *
 * @param turnId nulo em eventos fora de turno ({@code context.changed}, {@code conversation.renamed})
 */
public record NewEvent(
        UUID turnId,
        EventType type,
        Map<String, Object> payload
) {

    public NewEvent {
        if (isNull(type)) {
            throw new IllegalArgumentException("Evento sem tipo");
        }

        payload = isNull(payload) ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }
}
