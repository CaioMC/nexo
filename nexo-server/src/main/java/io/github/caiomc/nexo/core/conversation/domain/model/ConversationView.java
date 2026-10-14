package io.github.caiomc.nexo.core.conversation.domain.model;

/**
 * Conversa com o turno ativo, se houver (o front decide se mostra "Parar").
 *
 * @param activeTurn nulo quando não há turno em andamento
 */
public record ConversationView(
        Conversation conversation,
        Turn activeTurn
) {
}
