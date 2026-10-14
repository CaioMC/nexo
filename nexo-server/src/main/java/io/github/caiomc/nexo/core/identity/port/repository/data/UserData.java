package io.github.caiomc.nexo.core.identity.port.repository.data;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha de {@code app_user}.
 */
public record UserData(
        UUID id,
        String googleSubject,
        String email,
        String displayName,
        String pictureUrl,
        Instant createdAt,
        Instant lastLoginAt
) {
}
