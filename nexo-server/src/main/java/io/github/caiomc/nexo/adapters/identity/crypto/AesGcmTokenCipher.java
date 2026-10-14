package io.github.caiomc.nexo.adapters.identity.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import io.github.caiomc.nexo.core.identity.domain.model.EncryptedToken;
import io.github.caiomc.nexo.core.identity.port.crypto.TokenCipherPort;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

/**
 * AES-256-GCM (doc 04, seção 6): IV aleatório de 12 bytes por cifra, guardado na frente do texto
 * cifrado ({@code iv ‖ ciphertext ‖ tag}); o id do usuário é o dado associado (AAD).
 */
public class AesGcmTokenCipher implements TokenCipherPort {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final Map<Integer, SecretKey> keys;
    private final int currentVersion;
    private final SecureRandom random = new SecureRandom();

    public AesGcmTokenCipher(
            Map<Integer, SecretKey> keys,
            int currentVersion
    ) {
        if (!keys.containsKey(currentVersion)) {
            throw new IllegalArgumentException("Não há chave para a versão atual " + currentVersion + " (NEXO_TOKEN_KEY_VERSION)");
        }

        this.keys = Map.copyOf(keys);
        this.currentVersion = currentVersion;
    }

    @Override
    public EncryptedToken encrypt(
            String token,
            UserId owner
    ) {
        byte[] iv = new byte[IV_BYTES];

        this.random.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(Cipher.ENCRYPT_MODE, this.keys.get(this.currentVersion), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(this.aad(owner));

            byte[] sealed = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = ByteBuffer.allocate(iv.length + sealed.length).put(iv).put(sealed).array();

            return new EncryptedToken(bytes, this.currentVersion);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar o refresh token", e);
        }
    }

    @Override
    public String decrypt(
            EncryptedToken token,
            UserId owner
    ) {
        SecretKey key = this.keys.get(token.keyVersion());

        if (isNull(key)) {
            throw new IllegalStateException("Sem chave para decifrar a versão " + token.keyVersion() + " (NEXO_TOKEN_KEY_V" + token.keyVersion() + ")");
        }

        byte[] bytes = token.bytes();

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, Arrays.copyOfRange(bytes, 0, IV_BYTES)));
            cipher.updateAAD(this.aad(owner));

            return new String(cipher.doFinal(bytes, IV_BYTES, bytes.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Refresh token não decifra para o usuário " + owner + ": chave errada ou token de outro usuário", e);
        }
    }

    @Override
    public int currentKeyVersion() {
        return this.currentVersion;
    }

    private byte[] aad(UserId owner) {
        return owner.value().toString().getBytes(StandardCharsets.UTF_8);
    }
}
