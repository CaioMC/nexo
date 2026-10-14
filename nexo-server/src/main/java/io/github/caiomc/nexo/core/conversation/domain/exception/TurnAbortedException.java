package io.github.caiomc.nexo.core.conversation.domain.exception;

import io.github.caiomc.nexo.core.conversation.domain.enums.FailureCode;

/**
 * Interrompe o laço do agente; o {@code TurnConductorService} transforma em {@code turn.failed}.
 */
public class TurnAbortedException extends RuntimeException {

    private final FailureCode code;

    public TurnAbortedException(FailureCode code) {
        super(code.message());
        this.code = code;
    }

    public FailureCode code() {
        return this.code;
    }
}
