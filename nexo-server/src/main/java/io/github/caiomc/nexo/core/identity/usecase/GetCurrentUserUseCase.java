package io.github.caiomc.nexo.core.identity.usecase;

import io.github.caiomc.nexo.core.identity.domain.model.User;
import io.github.caiomc.nexo.core.identity.usecase.command.GetCurrentUserCommand;

public interface GetCurrentUserUseCase {

    User get(GetCurrentUserCommand command);
}
