package io.github.caiomc.nexo.core.conversation.usecase;

import io.github.caiomc.nexo.core.conversation.usecase.command.WatchConversationCommand;

public interface WatchConversationUseCase {

    /**
     * Entrega os eventos com {@code seq > lastSeq} já gravados e depois os novos, ao vivo, sem
     * buraco nem repetição (RF-22).
     */
    ConversationWatch watch(
            WatchConversationCommand command,
            ConversationEventListener listener
    );
}
