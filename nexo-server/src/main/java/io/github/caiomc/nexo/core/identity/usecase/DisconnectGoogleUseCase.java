package io.github.caiomc.nexo.core.identity.usecase;

import io.github.caiomc.nexo.core.identity.usecase.command.DisconnectGoogleCommand;

public interface DisconnectGoogleUseCase {

    void disconnect(DisconnectGoogleCommand command);
}
