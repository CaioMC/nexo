package io.github.caiomc.nexo.core.conversation.usecase.command;

import java.util.UUID;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

public record StopTurnCommand(
        UserId userId,
        UUID conversationId
) {

    public StopTurnCommand {
        if (isNull(userId)) {
            throw new IllegalArgumentException("Informe o usuário");
        }

        if (isNull(conversationId)) {
            throw new IllegalArgumentException("Informe a conversa");
        }
    }
}
