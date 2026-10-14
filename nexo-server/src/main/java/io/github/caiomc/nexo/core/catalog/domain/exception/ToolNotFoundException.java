package io.github.caiomc.nexo.core.catalog.domain.exception;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;

public class ToolNotFoundException extends RuntimeException {

    public ToolNotFoundException(ToolName name) {
        super("Ferramenta não encontrada no catálogo: " + name);
    }
}
