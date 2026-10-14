package io.github.caiomc.nexo.core.workspace.port.repository.data;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha de {@code user_context}.
 */
public record UserContextData(
        UUID userId,
        String contextKey,
        boolean enabled,
        Instant changedAt
) {
}
