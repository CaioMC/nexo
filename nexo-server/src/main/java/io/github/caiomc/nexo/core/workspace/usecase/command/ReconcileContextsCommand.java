package io.github.caiomc.nexo.core.workspace.usecase.command;

import java.util.Set;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

/**
 * Os escopos concedidos mudaram (refresh, novo login ou conta desconectada).
 *
 * @param grantedScopes vazio = conta desconectada (desliga tudo)
 */
public record ReconcileContextsCommand(
        UserId userId,
        Set<String> grantedScopes
) {

    public ReconcileContextsCommand {
        if (isNull(userId)) {
            throw new IllegalArgumentException("Informe o usuário");
        }

        grantedScopes = isNull(grantedScopes) ? Set.of() : Set.copyOf(grantedScopes);
    }
}
