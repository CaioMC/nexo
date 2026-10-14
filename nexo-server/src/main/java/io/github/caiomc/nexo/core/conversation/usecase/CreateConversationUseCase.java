package io.github.caiomc.nexo.core.conversation.usecase;

import io.github.caiomc.nexo.core.conversation.domain.model.Conversation;
import io.github.caiomc.nexo.core.conversation.usecase.command.CreateConversationCommand;

public interface CreateConversationUseCase {

    Conversation create(CreateConversationCommand command);
}
