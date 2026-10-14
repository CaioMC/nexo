package io.github.caiomc.nexo.core.conversation.usecase.command;

import java.util.UUID;

import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalDecision;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

public record DecideApprovalCommand(
        UserId userId,
        UUID conversationId,
        UUID approvalId,
        ApprovalDecision decision
) {

    public DecideApprovalCommand {
        if (isNull(userId)) {
            throw new IllegalArgumentException("Informe o usuário");
        }

        if (isNull(conversationId)) {
            throw new IllegalArgumentException("Informe a conversa");
        }

        if (isNull(approvalId) || isNull(decision)) {
            throw new IllegalArgumentException("Informe approvalId e decision");
        }
    }
}
