package io.github.caiomc.nexo.core.conversation.port.model.command;

/**
 * Uma mensagem de turnos anteriores que vai no {@code input} do modelo.
 *
 * @param role {@code user} ou {@code assistant}
 */
public record HistoryMessageCommand(
        String role,
        String text
) {

    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";
}
