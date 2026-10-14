package io.github.caiomc.nexo.core.conversation.usecase.command;

import java.util.UUID;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

public record SendMessageCommand(
        UserId userId,
        UUID conversationId,
        UUID clientMessageId,
        String text,
        String timeZone
) {

    public SendMessageCommand {
        if (isNull(userId)) {
            throw new IllegalArgumentException("Informe o usuário");
        }

        if (isNull(conversationId)) {
            throw new IllegalArgumentException("Informe a conversa");
        }

        if (isNull(clientMessageId)) {
            throw new IllegalArgumentException("Mensagem sem id do cliente (clientMessageId)");
        }

        if (isNull(text) || text.isBlank() || text.length() > 8000) {
            throw new IllegalArgumentException("A mensagem precisa ter de 1 a 8000 caracteres");
        }
    }
}
