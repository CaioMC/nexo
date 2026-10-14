package io.github.caiomc.nexo.adapters.identity.crypto;

import java.util.Map;
import java.util.UUID;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import io.github.caiomc.nexo.core.identity.domain.model.EncryptedToken;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmTokenCipherTest {

    private final UserId ana = UserId.of(UUID.randomUUID());
    private final UserId bruno = UserId.of(UUID.randomUUID());

    @Test
    void decryptsOnlyForTheOwnerAndNeverPrintsTheToken() throws Exception {
        AesGcmTokenCipher cipher = new AesGcmTokenCipher(Map.of(1, this.key()), 1);
        EncryptedToken first = cipher.encrypt("1//refresh-secreto", this.ana);
        EncryptedToken second = cipher.encrypt("1//refresh-secreto", this.ana);

        assertThat(cipher.decrypt(first, this.ana)).isEqualTo("1//refresh-secreto");
        assertThat(first.bytes()).isNotEqualTo(second.bytes());
        assertThat(first.toString()).doesNotContain("refresh").isEqualTo("EncryptedToken[***]");
        assertThatThrownBy(() -> cipher.decrypt(first, this.bruno)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void oldKeysStillDecryptAfterRotation() throws Exception {
        SecretKey old = this.key();
        EncryptedToken before = new AesGcmTokenCipher(Map.of(1, old), 1).encrypt("token", this.ana);
        AesGcmTokenCipher rotated = new AesGcmTokenCipher(Map.of(1, old, 2, this.key()), 2);

        assertThat(rotated.decrypt(before, this.ana)).isEqualTo("token");
        assertThat(rotated.encrypt("token", this.ana).keyVersion()).isEqualTo(2);
    }

    private SecretKey key() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");

        generator.init(256);

        return generator.generateKey();
    }
}
