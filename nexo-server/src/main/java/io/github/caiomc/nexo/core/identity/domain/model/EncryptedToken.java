package io.github.caiomc.nexo.core.identity.domain.model;

import java.util.Arrays;

import static java.util.Objects.isNull;

/**
 * Refresh token cifrado ({@code iv ‖ ciphertext ‖ tag}) e a versão da chave que o cifrou. O
 * conteúdo nunca aparece no {@link #toString()}.
 */
public record EncryptedToken(
        byte[] bytes,
        int keyVersion
) {

    public EncryptedToken {
        if (isNull(bytes) || bytes.length == 0) {
            throw new IllegalArgumentException("Token cifrado vazio");
        }

        if (keyVersion < 1) {
            throw new IllegalArgumentException("Versão da chave precisa ser positiva, veio " + keyVersion);
        }

        bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return this.bytes.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EncryptedToken token && token.keyVersion == this.keyVersion && Arrays.equals(token.bytes, this.bytes);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(this.bytes) + this.keyVersion;
    }

    @Override
    public String toString() {
        return "EncryptedToken[***]";
    }
}
