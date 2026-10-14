package io.github.caiomc.nexo.core.conversation.domain.enums;

/**
 * {@code BLOCKED} · {@code WAITING_APPROVAL → REJECTED | RUNNING → SUCCEEDED | FAILED}.
 */
public enum ToolCallStatus {

    BLOCKED,
    WAITING_APPROVAL,
    REJECTED,
    RUNNING,
    SUCCEEDED,
    FAILED
}
