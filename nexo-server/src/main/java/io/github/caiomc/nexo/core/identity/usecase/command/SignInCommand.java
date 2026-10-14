package io.github.caiomc.nexo.core.identity.usecase.command;

import java.time.Instant;
import java.util.Set;

import static java.util.Objects.isNull;

/**
 * O que o callback do login com Google trouxe: identidade do ID token e os tokens.
 *
 * @param refreshToken nulo quando o Google não emite um novo (usuário já tinha consentido)
 * @param grantedScopes escopos que o access token cobre
 */
public record SignInCommand(
        String googleSubject,
        String email,
        String displayName,
        String pictureUrl,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Set<String> grantedScopes
) {

    public SignInCommand {
        if (isNull(googleSubject) || googleSubject.isBlank()) {
            throw new IllegalArgumentException("O ID token do Google veio sem sub");
        }

        grantedScopes = isNull(grantedScopes) ? Set.of() : Set.copyOf(grantedScopes);
    }

    @Override
    public String toString() {
        return "SignInCommand[googleSubject=" + this.googleSubject + ", email=" + this.email + ", tokens=***]";
    }
}
