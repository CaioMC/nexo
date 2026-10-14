package io.github.caiomc.nexo.core.workspace.application;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.workspace.domain.exception.ContextNotFoundException;
import io.github.caiomc.nexo.core.workspace.domain.model.GoogleContext;
import io.github.caiomc.nexo.core.workspace.mapper.GoogleContextDataMapper;
import io.github.caiomc.nexo.core.workspace.port.repository.GoogleContextRepository;
import io.github.caiomc.nexo.core.workspace.usecase.FindContextUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.command.FindContextCommand;

import static java.util.Objects.isNull;

/**
 * Os contextos do seed (V8). Lidos uma vez, na primeira consulta: só mudam com migration nova.
 */
@Service
public class ContextDirectoryService implements FindContextUseCase {

    private final GoogleContextRepository repository;
    private final GoogleContextDataMapper mapper;

    private final AtomicReference<List<GoogleContext>> contexts = new AtomicReference<>();

    public ContextDirectoryService(
            GoogleContextRepository repository,
            GoogleContextDataMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<GoogleContext> all() {
        List<GoogleContext> loaded = this.contexts.get();

        if (isNull(loaded)) {
            loaded = this.mapper.toContexts(this.repository.contexts(), this.repository.scopes());
            this.contexts.set(loaded);
        }

        return loaded;
    }

    @Override
    public GoogleContext find(FindContextCommand command) {
        return this.all()
                .stream()
                .filter(context -> context.key().value().equals(command.context()))
                .findFirst()
                .orElseThrow(() -> new ContextNotFoundException(command.context()));
    }
}
