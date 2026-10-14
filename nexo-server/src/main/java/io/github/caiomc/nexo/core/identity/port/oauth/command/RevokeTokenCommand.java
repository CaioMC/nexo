package io.github.caiomc.nexo.core.identity.port.oauth.command;

/**
 * Revogação do token no Google (desconectar a conta). Revogar o refresh token revoga todo o acesso.
 */
public record RevokeTokenCommand(String token) {

    @Override
    public String toString() {
        return "RevokeTokenCommand[***]";
    }
}
