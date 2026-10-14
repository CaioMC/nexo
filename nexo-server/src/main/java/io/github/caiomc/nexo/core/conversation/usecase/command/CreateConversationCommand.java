package io.github.caiomc.nexo.core.conversation.usecase.command;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

public record CreateConversationCommand(
        UserId userId,
        String title
) {

    public CreateConversationCommand {
        if (isNull(userId)) {
            throw new IllegalArgumentException("Informe o usuário");
        }
    }
}
