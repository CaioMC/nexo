package io.github.caiomc.nexo.core.conversation.usecase.command;

import java.util.UUID;

import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalStatus;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

public record ListApprovalsCommand(
        UserId userId,
        UUID conversationId,
        ApprovalStatus status
) {

    public ListApprovalsCommand {
        if (isNull(userId)) {
            throw new IllegalArgumentException("Informe o usuário");
        }

        if (isNull(conversationId)) {
            throw new IllegalArgumentException("Informe a conversa");
        }
    }
}
