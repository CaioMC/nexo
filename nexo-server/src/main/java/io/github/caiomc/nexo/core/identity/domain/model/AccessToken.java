package io.github.caiomc.nexo.core.identity.domain.model;

import java.time.Duration;
import java.time.Instant;

import static java.util.Objects.isNull;

/**
 * Access token do Google, só em memória (nunca gravado). O valor nunca aparece no
 * {@link #toString()}.
 */
public record AccessToken(
        String value,
        Instant expiresAt,
        GrantedScopes scopes
) {

    public AccessToken {
        if (isNull(value) || value.isBlank()) {
            throw new IllegalArgumentException("Access token vazio");
        }

        if (isNull(expiresAt)) {
            throw new IllegalArgumentException("Access token sem validade");
        }

        scopes = isNull(scopes) ? GrantedScopes.none() : scopes;
    }

    /**
     * Ainda vale por mais que {@code margin} a partir de {@code now}.
     */
    public boolean validFor(
            Instant now,
            Duration margin
    ) {
        return now.plus(margin).isBefore(this.expiresAt);
    }

    @Override
    public String toString() {
        return "AccessToken[***, expiresAt=" + this.expiresAt + "]";
    }
}
