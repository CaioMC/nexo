package io.github.caiomc.nexo.core.identity.port.oauth.data;

import java.util.Set;

/**
 * Resposta do endpoint de token do Google.
 *
 * @param scopes escopos que o token cobre (o Google devolve a soma dos concedidos)
 */
public record TokenData(
        String accessToken,
        long expiresInSeconds,
        Set<String> scopes
) {

    @Override
    public String toString() {
        return "TokenData[accessToken=***, expiresInSeconds=" + this.expiresInSeconds + ", scopes=" + this.scopes + "]";
    }
}
