package io.github.caiomc.nexo.core.identity.domain.model;

import java.time.Instant;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

/**
 * O que o servidor guarda para agir em nome do usuário: só o refresh token cifrado e os escopos.
 */
public record GoogleCredential(
        UserId userId,
        EncryptedToken refreshToken,
        GrantedScopes scopes,
        Instant updatedAt
) {

    public GoogleCredential {
        if (isNull(userId) || isNull(refreshToken)) {
            throw new IllegalArgumentException("Credencial Google precisa de usuário e refresh token");
        }

        scopes = isNull(scopes) ? GrantedScopes.none() : scopes;
    }

    public GoogleCredential withScopes(
            GrantedScopes granted,
            Instant now
    ) {
        return new GoogleCredential(
                this.userId,
                this.refreshToken,
                granted,
                now
        );
    }

    /**
     * Mesmo token, cifrado de novo com a chave atual (rotação de chave).
     */
    public GoogleCredential reencrypted(
            EncryptedToken token,
            Instant now
    ) {
        return new GoogleCredential(
                this.userId,
                token,
                this.scopes,
                now
        );
    }
}
