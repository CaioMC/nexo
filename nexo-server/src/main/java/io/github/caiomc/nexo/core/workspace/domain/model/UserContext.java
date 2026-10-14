package io.github.caiomc.nexo.core.workspace.domain.model;

import java.time.Instant;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

/**
 * Estado de um contexto para um usuário. Ausência de linha = desligado.
 */
public record UserContext(
        UserId userId,
        ContextKey context,
        boolean enabled,
        Instant changedAt
) {

    public UserContext {
        if (isNull(userId) || isNull(context) || isNull(changedAt)) {
            throw new IllegalArgumentException("Estado de contexto precisa de usuário, contexto e data");
        }
    }

    public static UserContext switched(
            UserId userId,
            ContextKey context,
            boolean enabled,
            Instant now
    ) {
        return new UserContext(
                userId,
                context,
                enabled,
                now
        );
    }
}
