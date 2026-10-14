package io.github.caiomc.nexo.core.workspace.application;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.workspace.domain.model.ContextState;
import io.github.caiomc.nexo.core.workspace.domain.model.GoogleContext;
import io.github.caiomc.nexo.core.workspace.mapper.UserContextDataMapper;
import io.github.caiomc.nexo.core.workspace.mapper.WorkspaceCommandMapper;
import io.github.caiomc.nexo.core.workspace.port.notification.ContextChangeNotifierPort;
import io.github.caiomc.nexo.core.workspace.port.repository.UserContextRepository;
import io.github.caiomc.nexo.core.workspace.usecase.DisableContextUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.command.DisableContextCommand;

/**
 * Desligar um contexto. Não chama o Google (RN-09): o Google só revoga o acesso inteiro.
 */
@Service
public class DisableContextService implements DisableContextUseCase {

    private static final Logger log = LoggerFactory.getLogger(DisableContextService.class);

    private final ContextDirectoryService directory;
    private final ListContextsService states;
    private final UserContextRepository contexts;
    private final ContextChangeNotifierPort notifier;

    private final UserContextDataMapper mapper;
    private final WorkspaceCommandMapper commands;
    private final Clock clock;

    public DisableContextService(
            ContextDirectoryService directory,
            ListContextsService states,
            UserContextRepository contexts,
            ContextChangeNotifierPort notifier,
            UserContextDataMapper mapper,
            WorkspaceCommandMapper commands,
            Clock clock
    ) {
        this.directory = directory;
        this.states = states;
        this.contexts = contexts;
        this.notifier = notifier;
        this.mapper = mapper;
        this.commands = commands;
        this.clock = clock;
    }

    @Override
    public ContextState disable(DisableContextCommand command) {
        GoogleContext context = this.directory.find(this.commands.toFind(command.context()));

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
        log.info("Contexto {} desligado para {}", context.key(), command.userId());

        return this.states.state(command.userId(), context);
    }
}
