package io.github.caiomc.nexo.core.conversation.domain.exception;

import java.util.UUID;

/**
 * RF-25: uma conversa aceita um turno por vez.
 */
public class TurnInProgressException extends RuntimeException {

    public TurnInProgressException(UUID conversationId) {
        super("Já existe um turno em andamento na conversa " + conversationId + ".");
    }
}
