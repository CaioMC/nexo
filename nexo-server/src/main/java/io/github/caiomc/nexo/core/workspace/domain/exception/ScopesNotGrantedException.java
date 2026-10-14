package io.github.caiomc.nexo.core.workspace.domain.exception;

import java.util.Collection;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

/**
 * O consentimento voltou sem todos os escopos do contexto (RN-01): ele continua desligado.
 */
public class ScopesNotGrantedException extends RuntimeException {

    public ScopesNotGrantedException(
            ContextKey context,
            Collection<String> missing
    ) {
        super("O Google não concedeu todos os escopos de " + context + ". Faltaram: " + missing);
    }
}
