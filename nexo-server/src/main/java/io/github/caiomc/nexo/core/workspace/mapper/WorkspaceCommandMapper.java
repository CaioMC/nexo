package io.github.caiomc.nexo.core.workspace.mapper;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.identity.usecase.command.GetGrantedScopesCommand;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.workspace.usecase.command.FindContextCommand;
import io.github.caiomc.nexo.core.workspace.usecase.command.GetEnabledContextsCommand;
import io.github.caiomc.nexo.core.workspace.usecase.command.ListContextsCommand;

/**
 * Comandos que os serviços de contexto mandam a outros casos de uso.
 */
@Component
public class WorkspaceCommandMapper {

    public FindContextCommand toFind(String context) {
        return new FindContextCommand(context);
    }

    public GetEnabledContextsCommand toEnabled(UserId userId) {
        return new GetEnabledContextsCommand(userId);
    }

    public ListContextsCommand toList(UserId userId) {
        return new ListContextsCommand(userId);
    }

    public GetGrantedScopesCommand toGrantedScopes(UserId userId) {
        return new GetGrantedScopesCommand(userId);
    }
}
