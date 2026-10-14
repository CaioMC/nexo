package io.github.caiomc.nexo.adapters.identity.config;

import java.net.URI;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code nexo.identity.*}.
 *
 * @param tokenKey          chave AES-256 em Base64 (32 bytes) que cifra os refresh tokens; vazia
 *                          = chave aleatória a cada subida (só para desenvolvimento)
 * @param tokenKeyVersion   versão da chave atual
 * @param previousTokenKeys chaves antigas, por versão, só para decifrar (rotação)
 * @param webOrigin         origem do front: destino depois do login e única origem aceita no WebSocket
 */
@ConfigurationProperties(prefix = "nexo.identity")
public record IdentityProperties(
        String tokenKey,
        int tokenKeyVersion,
        Map<Integer, String> previousTokenKeys,
        String webOrigin,
        URI googleTokenUri,
        URI googleRevokeUri
) {
}
