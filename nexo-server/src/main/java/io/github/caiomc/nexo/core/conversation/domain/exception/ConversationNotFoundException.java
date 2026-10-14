package io.github.caiomc.nexo.core.conversation.domain.exception;

import java.util.UUID;

/**
 * Não existe ou é de outro usuário (RN-10): a mensagem não diz qual dos dois.
 */
public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(UUID id) {
        super("Conversa não encontrada: " + id);
    }
}
