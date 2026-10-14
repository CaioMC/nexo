package io.github.caiomc.nexo.core.retrieval.mapper;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.workspace.usecase.command.GetEnabledContextsCommand;

/**
 * Comandos que a busca manda a outros contextos.
 */
@Component
public class RetrievalCommandMapper {

    public GetEnabledContextsCommand toEnabledContexts(UserId userId) {
        return new GetEnabledContextsCommand(userId);
    }
}
