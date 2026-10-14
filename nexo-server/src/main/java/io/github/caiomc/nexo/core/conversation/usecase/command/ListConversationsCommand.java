package io.github.caiomc.nexo.core.conversation.usecase.command;

import java.time.Instant;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

public record ListConversationsCommand(
        UserId userId,
        Instant before,
        int limit
) {

    public ListConversationsCommand {
        if (isNull(userId)) {
            throw new IllegalArgumentException("Informe o usuário");
        }

        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit precisa estar entre 1 e 100, veio " + limit);
        }
    }
}
