package io.github.caiomc.nexo.core.identity.usecase;

import io.github.caiomc.nexo.core.identity.domain.model.AccessToken;
import io.github.caiomc.nexo.core.identity.usecase.command.ObtainAccessTokenCommand;

public interface ObtainAccessTokenUseCase {

    AccessToken obtain(ObtainAccessTokenCommand command);

    /**
     * Esquece o access token em cache (o Google respondeu 401 com ele).
     */
    void invalidate(ObtainAccessTokenCommand command);
}
