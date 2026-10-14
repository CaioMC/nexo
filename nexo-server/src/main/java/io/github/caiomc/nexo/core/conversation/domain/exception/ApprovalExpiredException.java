package io.github.caiomc.nexo.core.conversation.domain.exception;

import java.util.UUID;

/**
 * RN-06: aprovação vencida conta como recusa.
 */
public class ApprovalExpiredException extends RuntimeException {

    public ApprovalExpiredException(UUID id) {
        super("A aprovação " + id + " venceu: o pedido foi tratado como recusado.");
    }
}
