package io.github.caiomc.nexo.adapters.conversation.websocket.message;

import java.util.Map;

/**
 * {@code {"kind": "command", "id": "...", "type": "user.message", "payload": {...}}} (doc 06, seção 2.2).
 */
public record ClientCommandMessage(
        String kind,
        String id,
        String type,
        Map<String, Object> payload
) {

    public static final String COMMAND = "command";
    public static final String USER_MESSAGE = "user.message";
    public static final String APPROVAL_DECIDE = "approval.decide";
    public static final String TURN_STOP = "turn.stop";
}
