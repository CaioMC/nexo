package io.github.caiomc.nexo.core.workspace.domain.model;

import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

/**
 * Resultado de pedir para ligar um contexto.
 */
public sealed interface ContextActivation {

    ContextKey context();

    /**
     * Os escopos já estavam concedidos: ligado na hora (RF-12).
     */
    record Enabled(ContextKey context) implements ContextActivation {
    }

    /**
     * Faltam escopos: o navegador vai ao Google pedir só os deste contexto (RF-11).
     */
    record ConsentRequired(
            ContextKey context,
            String authorizationUrl
    ) implements ContextActivation {
    }
}
