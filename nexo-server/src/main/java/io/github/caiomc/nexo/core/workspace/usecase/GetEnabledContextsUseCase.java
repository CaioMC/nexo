package io.github.caiomc.nexo.core.workspace.usecase;

import java.util.Set;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.workspace.usecase.command.GetEnabledContextsCommand;

public interface GetEnabledContextsUseCase {

    /**
     * Contextos ligados agora (lê o banco a cada chamada: desligar vale na hora, RN-03).
     */
    Set<ContextKey> enabled(GetEnabledContextsCommand command);
}
