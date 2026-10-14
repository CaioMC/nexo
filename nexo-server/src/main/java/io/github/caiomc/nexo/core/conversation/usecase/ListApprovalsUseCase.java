package io.github.caiomc.nexo.core.conversation.usecase;

import java.util.List;

import io.github.caiomc.nexo.core.conversation.domain.model.ToolApproval;
import io.github.caiomc.nexo.core.conversation.usecase.command.ListApprovalsCommand;

public interface ListApprovalsUseCase {

    List<ToolApproval> list(ListApprovalsCommand command);
}
