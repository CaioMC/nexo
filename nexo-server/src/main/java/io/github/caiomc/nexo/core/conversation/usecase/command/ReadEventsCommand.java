package io.github.caiomc.nexo.core.conversation.usecase.command;

import java.util.UUID;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

public record ReadEventsCommand(
        UserId userId,
        UUID conversationId,
        long afterSeq,
        int limit
) {

    public ReadEventsCommand {
        if (isNull(userId)) {
            throw new IllegalArgumentException("Informe o usuário");
        }

        if (isNull(conversationId)) {
            throw new IllegalArgumentException("Informe a conversa");
        }

        if (afterSeq < 0) {
            throw new IllegalArgumentException("afterSeq não pode ser negativo");
        }

        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("limit precisa estar entre 1 e 1000, veio " + limit);
        }
    }
}
