package io.github.caiomc.nexo.core.conversation.usecase;

import io.github.caiomc.nexo.core.conversation.domain.model.AssistantEvent;

/**
 * Quem acompanha a conversa (um WebSocket aberto).
 */
public interface ConversationEventListener {

    void onEvent(AssistantEvent event);

    /**
     * A conversa foi apagada enquanto conectado.
     */
    default void onTerminated() {
    }
}
