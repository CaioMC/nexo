package io.github.caiomc.nexo.adapters.conversation.websocket.message;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AckMessage(
        String kind,
        String commandId,
        UUID turnId
) {

    public static final String ACK = "ack";
}
