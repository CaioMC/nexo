package io.github.caiomc.nexo.core.workspace.application;

import java.time.Clock;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.workspace.domain.model.GoogleContext;
import io.github.caiomc.nexo.core.workspace.mapper.UserContextDataMapper;
import io.github.caiomc.nexo.core.workspace.mapper.WorkspaceCommandMapper;
import io.github.caiomc.nexo.core.workspace.port.notification.ContextChangeNotifierPort;
import io.github.caiomc.nexo.core.workspace.port.repository.UserContextRepository;
import io.github.caiomc.nexo.core.workspace.usecase.GetEnabledContextsUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.ReconcileContextsUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.command.ReconcileContextsCommand;

/**
 * Mantém RN-01 quando os escopos mudam por fora: escopo sumiu no refresh, o Google negou acesso
 * ({@code insufficientPermissions}) ou a conta foi desconectada.
 */
@Service
public class ReconcileContextsService implements ReconcileContextsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReconcileContextsService.class);

    private final ContextDirectoryService directory;
    private final GetEnabledContextsUseCase enabled;
    private final UserContextRepository contexts;
    private final ContextChangeNotifierPort notifier;

    private final UserContextDataMapper mapper;
    private final WorkspaceCommandMapper commands;
    private final Clock clock;

    public ReconcileContextsService(
            ContextDirectoryService directory,
            GetEnabledContextsUseCase enabled,
            UserContextRepository contexts,
            ContextChangeNotifierPort notifier,
            UserContextDataMapper mapper,
            WorkspaceCommandMapper commands,
            Clock clock
    ) {
        this.directory = directory;
        this.enabled = enabled;
        this.contexts = contexts;
        this.notifier = notifier;
        this.mapper = mapper;
        this.commands = commands;
        this.clock = clock;
    }

    @Override
    public void reconcile(ReconcileContextsCommand command) {
        Set<ContextKey> on = this.enabled.enabled(this.commands.toEnabled(command.userId()));

        for (GoogleContext context : this.directory.all()) {
            if (!on.contains(context.key()) || context.grantedBy(command.grantedScopes())) {
                continue;
            }

            this.contexts.save(
                    this.mapper.toData(
                            command.userId(),
                            context.key(),
                            false,
                            this.clock.instant()
                    )
            );
            this.notifier.notify(
                    this.mapper.toNotifyCommand(
                            command.userId(),
                            context.key(),
                            false
                    )
            );
            log.info("Contexto {} desligado para {}: escopos não estão mais concedidos", context.key(), command.userId());
        }
    }
}
