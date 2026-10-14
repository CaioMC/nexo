package io.github.caiomc.nexo.core.conversation.usecase;

import io.github.caiomc.nexo.core.conversation.usecase.command.DeleteConversationCommand;

public interface DeleteConversationUseCase {

    void delete(DeleteConversationCommand command);
}
