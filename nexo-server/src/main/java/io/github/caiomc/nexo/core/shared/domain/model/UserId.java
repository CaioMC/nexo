package io.github.caiomc.nexo.core.shared.domain.model;

import java.util.UUID;

import static java.util.Objects.isNull;

/**
 * Identidade do usuário no Nexo (não é o {@code sub} do Google). Compartilhado entre os contextos
 * delimitados, como o {@link ContextKey}.
 */
public record UserId(UUID value) {

    public UserId {
        if (isNull(value)) {
            throw new IllegalArgumentException("Usuário sem id");
        }
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return this.value.toString();
    }
}
