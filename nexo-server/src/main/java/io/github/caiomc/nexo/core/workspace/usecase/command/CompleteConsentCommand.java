package io.github.caiomc.nexo.core.workspace.usecase.command;

import java.util.Set;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

/**
 * Volta do consentimento incremental.
 *
 * @param grantedScopes todos os escopos concedidos depois do consentimento
 */
public record CompleteConsentCommand(
        UserId userId,
        String context,
        Set<String> grantedScopes
) {

    public CompleteConsentCommand {
        if (isNull(userId)) {
            throw new IllegalArgumentException("Informe o usuário");
        }

        grantedScopes = isNull(grantedScopes) ? Set.of() : Set.copyOf(grantedScopes);
    }
}
