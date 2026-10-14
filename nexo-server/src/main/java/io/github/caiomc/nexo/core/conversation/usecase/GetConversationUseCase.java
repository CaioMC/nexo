package io.github.caiomc.nexo.core.conversation.usecase;

import io.github.caiomc.nexo.core.conversation.domain.model.ConversationView;
import io.github.caiomc.nexo.core.conversation.usecase.command.GetConversationCommand;

public interface GetConversationUseCase {

    ConversationView get(GetConversationCommand command);
}
