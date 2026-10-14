package io.github.caiomc.nexo.core.conversation.usecase;

import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.usecase.command.SendMessageCommand;

public interface SendMessageUseCase {

    /**
     * Grava a mensagem e abre o turno; repetir o mesmo {@code clientMessageId} devolve o mesmo
     * turno sem criar outro (RF-26).
     */
    Turn send(SendMessageCommand command);
}
