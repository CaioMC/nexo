package io.github.caiomc.nexo.core.conversation.port.tool.data;

/**
 * Como terminou a execução no Google.
 */
public enum ToolOutcome {

    SUCCEEDED,

    /**
     * Erro do Google ou dos argumentos: vai ao modelo como erro.
     */
    FAILED,

    /**
     * Refresh token recusado: o turno falha com {@code GOOGLE_RECONNECT_REQUIRED}.
     */
    RECONNECT_REQUIRED,

    /**
     * 403 {@code insufficientPermissions}: o escopo sumiu, o contexto é desligado.
     */
    PERMISSION_DENIED
}
