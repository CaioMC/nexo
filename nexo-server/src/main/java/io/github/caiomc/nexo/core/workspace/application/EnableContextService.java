package io.github.caiomc.nexo.core.workspace.application;

import java.time.Clock;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.identity.usecase.GetGrantedScopesUseCase;
import io.github.caiomc.nexo.core.workspace.domain.model.ContextActivation;
import io.github.caiomc.nexo.core.workspace.domain.model.GoogleContext;
import io.github.caiomc.nexo.core.workspace.mapper.UserContextDataMapper;
import io.github.caiomc.nexo.core.workspace.mapper.WorkspaceCommandMapper;
import io.github.caiomc.nexo.core.workspace.port.consent.ConsentUrlPort;
import io.github.caiomc.nexo.core.workspace.port.notification.ContextChangeNotifierPort;
import io.github.caiomc.nexo.core.workspace.port.repository.UserContextRepository;
import io.github.caiomc.nexo.core.workspace.usecase.EnableContextUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.command.EnableContextCommand;

/**
 * Ligar um contexto: se os escopos já foram concedidos, liga na hora (RF-12); senão devolve a URL
 * do consentimento incremental (RF-11) e só liga na volta ({@code CompleteConsentService}).
 */
@Service
public class EnableContextService implements EnableContextUseCase {

    private static final Logger log = LoggerFactory.getLogger(EnableContextService.class);

    private final ContextDirectoryService directory;
    private final UserContextRepository contexts;
    private final GetGrantedScopesUseCase scopes;
    private final ConsentUrlPort consent;
    private final ContextChangeNotifierPort notifier;

    private final UserContextDataMapper mapper;
    private final WorkspaceCommandMapper commands;
    private final Clock clock;

    public EnableContextService(
            ContextDirectoryService directory,
            UserContextRepository contexts,
            GetGrantedScopesUseCase scopes,
            ConsentUrlPort consent,
            ContextChangeNotifierPort notifier,
            UserContextDataMapper mapper,
            WorkspaceCommandMapper commands,
            Clock clock
    ) {
        this.directory = directory;
        this.contexts = contexts;
        this.scopes = scopes;
        this.consent = consent;
        this.notifier = notifier;
        this.mapper = mapper;
        this.commands = commands;
        this.clock = clock;
    }

    @Override
    public ContextActivation enable(EnableContextCommand command) {
        GoogleContext context = this.directory.find(this.commands.toFind(command.context()));
        Set<String> granted = this.scopes.granted(this.commands.toGrantedScopes(command.userId()));

        if (!context.grantedBy(granted)) {
            return this.mapper.consentRequired(
                    context.key(),
                    this.consent.consentUrl(this.mapper.toConsentUrlCommand(context.key()))
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
        log.info("Contexto {} ligado para {}", context.key(), command.userId());

        return this.mapper.enabled(context.key());
    }
}
