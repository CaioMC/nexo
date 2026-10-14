package io.github.caiomc.nexo.core.workspace.usecase;

import io.github.caiomc.nexo.core.workspace.usecase.command.ReconcileContextsCommand;

public interface ReconcileContextsUseCase {

    /**
     * Desliga os contextos ligados cujos escopos não estão mais concedidos (RN-01).
     */
    void reconcile(ReconcileContextsCommand command);
}
