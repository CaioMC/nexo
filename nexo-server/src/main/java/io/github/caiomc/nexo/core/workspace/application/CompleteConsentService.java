package io.github.caiomc.nexo.core.workspace.application;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.workspace.domain.exception.ScopesNotGrantedException;
import io.github.caiomc.nexo.core.workspace.domain.model.GoogleContext;
import io.github.caiomc.nexo.core.workspace.mapper.UserContextDataMapper;
import io.github.caiomc.nexo.core.workspace.mapper.WorkspaceCommandMapper;
import io.github.caiomc.nexo.core.workspace.port.notification.ContextChangeNotifierPort;
import io.github.caiomc.nexo.core.workspace.port.repository.UserContextRepository;
import io.github.caiomc.nexo.core.workspace.usecase.CompleteConsentUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.command.CompleteConsentCommand;

/**
 * Volta do consentimento incremental: liga o contexto só se o Google concedeu todos os escopos
 * dele (RN-01). Concessão parcial deixa desligado.
 */
@Service
public class CompleteConsentService implements CompleteConsentUseCase {

    private static final Logger log = LoggerFactory.getLogger(CompleteConsentService.class);

    private final ContextDirectoryService directory;
    private final UserContextRepository contexts;
    private final ContextChangeNotifierPort notifier;

    private final UserContextDataMapper mapper;
    private final WorkspaceCommandMapper commands;
    private final Clock clock;

    public CompleteConsentService(
            ContextDirectoryService directory,
            UserContextRepository contexts,
            ContextChangeNotifierPort notifier,
            UserContextDataMapper mapper,
            WorkspaceCommandMapper commands,
            Clock clock
    ) {
        this.directory = directory;
        this.contexts = contexts;
        this.notifier = notifier;
        this.mapper = mapper;
        this.commands = commands;
        this.clock = clock;
    }

    @Override
    public void complete(CompleteConsentCommand command) {
        GoogleContext context = this.directory.find(this.commands.toFind(command.context()));

        if (!context.grantedBy(command.grantedScopes())) {
            throw new ScopesNotGrantedException(
                    context.key(),
                    context.sortedScopes().stream().filter(scope -> !command.grantedScopes().contains(scope)).toList()
            );
        }

        this.contexts.save(
                this.mapper.toData(
                        command.userId(),
                        context.key(),
                        true,
                        this.clock.instant()
                )
        );
        this.notifier.notify(
                this.mapper.toNotifyCommand(
                        command.userId(),
                        context.key(),
                        true
                )
        );
        log.info("Consentimento concluído: contexto {} ligado para {}", context.key(), command.userId());
    }
}
