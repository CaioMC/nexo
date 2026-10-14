package io.github.caiomc.nexo.core.identity.usecase.command;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

public record GetCurrentUserCommand(UserId userId) {

    public GetCurrentUserCommand {
        if (isNull(userId)) {
            throw new IllegalArgumentException("Informe o usuário");
        }
    }
}
