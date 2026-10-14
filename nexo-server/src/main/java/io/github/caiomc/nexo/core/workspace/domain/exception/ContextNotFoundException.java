package io.github.caiomc.nexo.core.workspace.domain.exception;

public class ContextNotFoundException extends RuntimeException {

    public ContextNotFoundException(String key) {
        super("Contexto não encontrado: '" + key + "'. Use gmail, drive, docs, sheets, slides, calendar, chat ou people.");
    }
}
