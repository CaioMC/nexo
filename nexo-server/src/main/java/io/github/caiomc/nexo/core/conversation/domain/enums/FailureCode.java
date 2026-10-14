package io.github.caiomc.nexo.core.conversation.domain.enums;

/**
 * Por que um turno terminou sem resposta ({@code turn.failed}).
 */
public enum FailureCode {

    STOPPED("Turno interrompido pelo usuário."),
    LIMIT_MODEL_CALLS("O turno passou do limite de chamadas ao modelo."),
    LIMIT_TOOL_CALLS("O turno passou do limite de chamadas de ferramenta."),
    TIMEOUT("O turno passou do tempo máximo."),
    MODEL_ERROR("O modelo de linguagem falhou ou não respondeu."),
    GOOGLE_RECONNECT_REQUIRED("O acesso ao Google foi revogado ou venceu: reconecte sua conta."),
    SERVER_RESTARTED("O servidor reiniciou no meio do turno.");

    private final String message;

    FailureCode(String message) {
        this.message = message;
    }

    public String message() {
        return this.message;
    }
}
