package io.github.caiomc.nexo.core.identity.usecase;

import java.util.Set;

import io.github.caiomc.nexo.core.identity.usecase.command.GetGrantedScopesCommand;

public interface GetGrantedScopesUseCase {

    /**
     * Escopos já concedidos; vazio se não há credencial.
     */
    Set<String> granted(GetGrantedScopesCommand command);
}
