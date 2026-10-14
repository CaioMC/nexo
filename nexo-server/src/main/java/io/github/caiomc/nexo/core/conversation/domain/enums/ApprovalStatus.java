package io.github.caiomc.nexo.core.conversation.domain.enums;

/**
 * {@code PENDING → APPROVED → CONSUMED} · {@code PENDING → REJECTED | EXPIRED}.
 */
public enum ApprovalStatus {

    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED,
    CONSUMED
}
