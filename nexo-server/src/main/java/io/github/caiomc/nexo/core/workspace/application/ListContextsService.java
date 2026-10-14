package io.github.caiomc.nexo.core.workspace.application;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolCatalog;
import io.github.caiomc.nexo.core.catalog.usecase.ProvideCatalogUseCase;
import io.github.caiomc.nexo.core.identity.usecase.GetGrantedScopesUseCase;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.workspace.domain.model.ContextState;
import io.github.caiomc.nexo.core.workspace.domain.model.GoogleContext;
import io.github.caiomc.nexo.core.workspace.mapper.UserContextDataMapper;
import io.github.caiomc.nexo.core.workspace.mapper.WorkspaceCommandMapper;
import io.github.caiomc.nexo.core.workspace.usecase.GetEnabledContextsUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.ListContextsUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.command.ListContextsCommand;

/**
 * A tela de contextos (RF-10): cada contexto com estado, escopos concedidos e nº de ferramentas.
 */
@Service
public class ListContextsService implements ListContextsUseCase {

    private final ContextDirectoryService directory;
    private final GetEnabledContextsUseCase enabled;
    private final GetGrantedScopesUseCase scopes;
    private final ProvideCatalogUseCase catalog;
    private final UserContextDataMapper mapper;
    private final WorkspaceCommandMapper commands;

    public ListContextsService(
            ContextDirectoryService directory,
            GetEnabledContextsUseCase enabled,
            GetGrantedScopesUseCase scopes,
            ProvideCatalogUseCase catalog,
            UserContextDataMapper mapper,
            WorkspaceCommandMapper commands
    ) {
        this.directory = directory;
        this.enabled = enabled;
        this.scopes = scopes;
        this.catalog = catalog;
        this.mapper = mapper;
        this.commands = commands;
    }

    @Override
    public List<ContextState> list(ListContextsCommand command) {
        Set<ContextKey> on = this.enabled.enabled(this.commands.toEnabled(command.userId()));
        Set<String> granted = this.granted(command.userId());
        ToolCatalog tools = this.catalog.current();

        return this.directory.all()
                .stream()
                .map(context -> this.mapper.toState(
                        context,
                        on.contains(context.key()),
                        granted,
                        tools.filter(context.key(), null).size()
                ))
                .toList();
    }

    public ContextState state(
            UserId userId,
            GoogleContext context
    ) {
        return this.list(this.commands.toList(userId))
                .stream()
                .filter(state -> state.context().key().equals(context.key()))
                .findFirst()
                .orElseThrow();
    }

    private Set<String> granted(UserId userId) {
        return this.scopes.granted(this.commands.toGrantedScopes(userId));
    }
}
