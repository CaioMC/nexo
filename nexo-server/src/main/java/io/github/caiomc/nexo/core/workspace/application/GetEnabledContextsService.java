package io.github.caiomc.nexo.core.workspace.application;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.workspace.domain.model.UserContext;
import io.github.caiomc.nexo.core.workspace.mapper.UserContextDataMapper;
import io.github.caiomc.nexo.core.workspace.port.repository.UserContextRepository;
import io.github.caiomc.nexo.core.workspace.usecase.GetEnabledContextsUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.command.GetEnabledContextsCommand;

/**
 * Quais contextos o usuário deixou ligados. Lê o banco a cada chamada: é uma consulta pequena e é
 * o que garante que desligar vale na hora (RN-03).
 */
@Service
public class GetEnabledContextsService implements GetEnabledContextsUseCase {

    private final UserContextRepository repository;
    private final UserContextDataMapper mapper;

    public GetEnabledContextsService(
            UserContextRepository repository,
            UserContextDataMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Set<ContextKey> enabled(GetEnabledContextsCommand command) {
        return this.repository.findByUser(command.userId().value())
                .stream()
                .map(this.mapper::toUserContext)
                .filter(UserContext::enabled)
                .map(UserContext::context)
                .collect(Collectors.toUnmodifiableSet());
    }
}
