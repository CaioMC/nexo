package io.github.caiomc.nexo.core.workspace.domain.model;

/**
 * O que a tela de contextos mostra de cada um.
 *
 * @param scopesGranted todos os escopos do contexto já foram concedidos (ligar não precisa do Google)
 * @param toolCount     ferramentas do catálogo neste contexto
 */
public record ContextState(
        GoogleContext context,
        boolean enabled,
        boolean scopesGranted,
        int toolCount
) {
}
