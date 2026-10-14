package io.github.caiomc.nexo.core.identity.mapper;

import java.util.Set;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.workspace.usecase.command.ReconcileContextsCommand;

/**
 * Comandos que a identidade manda a outros contextos.
 */
@Component
public class IdentityCommandMapper {

    public ReconcileContextsCommand toReconcile(
            UserId userId,
            Set<String> grantedScopes
    ) {
        return new ReconcileContextsCommand(userId, grantedScopes);
    }

    /**
     * Conta desconectada ou acesso revogado: nenhum escopo vale mais.
     */
    public ReconcileContextsCommand toRevoked(UserId userId) {
        return new ReconcileContextsCommand(userId, Set.of());
    }
}
