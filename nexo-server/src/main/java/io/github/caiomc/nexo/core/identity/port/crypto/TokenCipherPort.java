package io.github.caiomc.nexo.core.identity.port.crypto;

import io.github.caiomc.nexo.core.identity.domain.model.EncryptedToken;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

/**
 * Cifra do refresh token. O usuário entra como dado associado (AAD): um token copiado para a linha
 * de outro usuário não decifra.
 */
public interface TokenCipherPort {

    EncryptedToken encrypt(
            String token,
            UserId owner
    );

    String decrypt(
            EncryptedToken token,
            UserId owner
    );

    /**
     * Versão da chave atual: token cifrado com outra é recifrado no próximo uso.
     */
    int currentKeyVersion();
}
