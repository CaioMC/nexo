package io.github.caiomc.nexo.core.conversation.domain.exception;

import java.util.UUID;

public class NoActiveTurnException extends RuntimeException {

    public NoActiveTurnException(UUID conversationId) {
        super("Não há turno em andamento na conversa " + conversationId + ".");
    }
}
