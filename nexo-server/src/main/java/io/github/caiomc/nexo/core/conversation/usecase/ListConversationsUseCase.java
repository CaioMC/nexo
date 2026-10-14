package io.github.caiomc.nexo.core.conversation.usecase;

import java.util.List;

import io.github.caiomc.nexo.core.conversation.domain.model.Conversation;
import io.github.caiomc.nexo.core.conversation.usecase.command.ListConversationsCommand;

public interface ListConversationsUseCase {

    List<Conversation> list(ListConversationsCommand command);
}
