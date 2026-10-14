package io.github.caiomc.nexo.core.identity.usecase.command;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

public record ObtainAccessTokenCommand(UserId userId) {

    public ObtainAccessTokenCommand {
        if (isNull(userId)) {
            throw new IllegalArgumentException("Informe o usuário");
        }
    }
}
