package io.github.caiomc.nexo.core.workspace.usecase;

import io.github.caiomc.nexo.core.workspace.domain.exception.ScopesNotGrantedException;
import io.github.caiomc.nexo.core.workspace.usecase.command.CompleteConsentCommand;

public interface CompleteConsentUseCase {

    /**
     * Liga o contexto se o Google concedeu todos os escopos dele.
     *
     * @throws ScopesNotGrantedException se faltou algum (o contexto continua desligado)
     */
    void complete(CompleteConsentCommand command);
}
