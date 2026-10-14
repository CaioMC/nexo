package io.github.caiomc.nexo.core.conversation.usecase;

import io.github.caiomc.nexo.core.conversation.usecase.command.StopTurnCommand;

public interface StopTurnUseCase {

    void stop(StopTurnCommand command);
}
