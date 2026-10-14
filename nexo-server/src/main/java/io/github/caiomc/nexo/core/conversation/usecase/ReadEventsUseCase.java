package io.github.caiomc.nexo.core.conversation.usecase;

import java.util.List;

import io.github.caiomc.nexo.core.conversation.domain.model.AssistantEvent;
import io.github.caiomc.nexo.core.conversation.usecase.command.ReadEventsCommand;

public interface ReadEventsUseCase {

    List<AssistantEvent> read(ReadEventsCommand command);
}
