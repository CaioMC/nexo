package io.github.caiomc.nexo.core.identity.port.oauth.command;

import io.github.caiomc.nexo.core.shared.domain.model.UserId;

/**
 * Pedido de um access token novo a partir do refresh token (já decifrado).
 */
public record RefreshTokenCommand(
        UserId userId,
        String refreshToken
) {

    @Override
    public String toString() {
        return "RefreshTokenCommand[userId=" + this.userId + ", refreshToken=***]";
    }
}
