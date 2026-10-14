package io.github.caiomc.nexo.core.conversation.usecase;

import io.github.caiomc.nexo.core.conversation.usecase.command.DecideApprovalCommand;

public interface DecideApprovalUseCase {

    void decide(DecideApprovalCommand command);
}
