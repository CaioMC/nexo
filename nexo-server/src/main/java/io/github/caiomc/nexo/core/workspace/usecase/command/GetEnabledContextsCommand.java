package io.github.caiomc.nexo.core.workspace.usecase.command;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

public record GetEnabledContextsCommand(UserId userId) {

    public GetEnabledContextsCommand {
        if (isNull(userId)) {
            throw new IllegalArgumentException("Informe o usuário");
        }
    }
}
