package io.github.caiomc.nexo.core.conversation.domain.exception;

import java.util.UUID;

public class ApprovalNotFoundException extends RuntimeException {

    public ApprovalNotFoundException(UUID id) {
        super("Aprovação não encontrada: " + id);
    }
}
