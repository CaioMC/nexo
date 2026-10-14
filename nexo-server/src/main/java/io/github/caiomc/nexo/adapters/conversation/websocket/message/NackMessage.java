package io.github.caiomc.nexo.adapters.conversation.websocket.message;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Comando recusado. {@code commandId} nulo quando nem deu para ler o JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NackMessage(
        String kind,
        String commandId,
        String code,
        String message
) {

    public static final String NACK = "nack";
}
