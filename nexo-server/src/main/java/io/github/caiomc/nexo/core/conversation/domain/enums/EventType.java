package io.github.caiomc.nexo.core.conversation.domain.enums;

import java.util.Arrays;

/**
 * Tipos de evento da conversa (doc 06, seção 4), com o nome que vai no fio.
 */
public enum EventType {

    MESSAGE_USER("message.user"),
    TURN_STARTED("turn.started"),
    ASSISTANT_DELTA("assistant.delta"),
    ASSISTANT_MESSAGE("assistant.message"),
    TOOL_SEARCH("tool.search"),
    CONTEXT_SUGGESTED("context.suggested"),
    TOOL_STARTED("tool.started"),
    TOOL_BLOCKED("tool.blocked"),
    APPROVAL_REQUESTED("approval.requested"),
    APPROVAL_RESOLVED("approval.resolved"),
    TOOL_COMPLETED("tool.completed"),
    USAGE_UPDATED("usage.updated"),
    TURN_COMPLETED("turn.completed"),
    TURN_FAILED("turn.failed"),
    CONTEXT_CHANGED("context.changed"),
    CONVERSATION_RENAMED("conversation.renamed");

    private final String wire;

    EventType(String wire) {
        this.wire = wire;
    }

    public static EventType fromWire(String wire) {
        return Arrays.stream(values())
                .filter(type -> type.wire.equals(wire))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de evento desconhecido: " + wire));
    }

    public String wire() {
        return this.wire;
    }
}
