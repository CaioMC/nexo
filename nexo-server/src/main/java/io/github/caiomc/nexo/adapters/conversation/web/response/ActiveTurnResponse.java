package io.github.caiomc.nexo.adapters.conversation.web.response;

import java.util.UUID;

public record ActiveTurnResponse(
        UUID id,
        String status
) {
}
