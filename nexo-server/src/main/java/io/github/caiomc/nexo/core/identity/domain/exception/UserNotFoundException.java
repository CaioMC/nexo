package io.github.caiomc.nexo.core.identity.domain.exception;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UserId userId) {
        super("Usuário não encontrado: " + userId);
    }
}
