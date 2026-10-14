package io.github.caiomc.nexo.core.conversation.domain.exception;

import java.util.UUID;

import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalStatus;

public class ApprovalNotPendingException extends RuntimeException {

    public ApprovalNotPendingException(
            UUID id,
            ApprovalStatus status
    ) {
        super("A aprovação " + id + " não está mais pendente (" + status + ").");
    }
}
