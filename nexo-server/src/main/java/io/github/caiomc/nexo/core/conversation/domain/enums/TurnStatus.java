package io.github.caiomc.nexo.core.conversation.domain.enums;

/**
 * {@code RUNNING ⇄ WAITING_APPROVAL → COMPLETED | FAILED} (doc 11, seção 2).
 */
public enum TurnStatus {

    RUNNING,
    WAITING_APPROVAL,
    COMPLETED,
    FAILED;

    public boolean isActive() {
        return this == RUNNING || this == WAITING_APPROVAL;
    }
}
