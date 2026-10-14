package io.github.caiomc.nexo.core.identity.port.oauth;

import io.github.caiomc.nexo.core.identity.domain.exception.ConsentRevokedException;
import io.github.caiomc.nexo.core.identity.port.oauth.command.RefreshTokenCommand;
import io.github.caiomc.nexo.core.identity.port.oauth.command.RevokeTokenCommand;
import io.github.caiomc.nexo.core.identity.port.oauth.data.TokenData;

/**
 * Endpoints OAuth do Google que o servidor chama fora do fluxo de login.
 */
public interface GoogleOAuthPort {

    /**
     * @throws ConsentRevokedException se o Google responder {@code invalid_grant}
     */
    TokenData refresh(RefreshTokenCommand command);

    /**
     * Revoga o token no Google. Falha de rede não impede o desconectar local.
     */
    void revoke(RevokeTokenCommand command);
}
