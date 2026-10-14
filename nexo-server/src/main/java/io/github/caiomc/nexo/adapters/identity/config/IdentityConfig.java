package io.github.caiomc.nexo.adapters.identity.config;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.caiomc.nexo.adapters.identity.crypto.AesGcmTokenCipher;
import io.github.caiomc.nexo.adapters.identity.google.GoogleOAuthAdapter;
import io.github.caiomc.nexo.core.identity.port.crypto.TokenCipherPort;
import io.github.caiomc.nexo.core.identity.port.oauth.GoogleOAuthPort;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Configuration
public class IdentityConfig {

    private static final Logger log = LoggerFactory.getLogger(IdentityConfig.class);

    private static final int KEY_BYTES = 32;

    @Bean
    TokenCipherPort tokenCipher(IdentityProperties identity) throws Exception {
        Map<Integer, SecretKey> keys = new HashMap<>();

        if (isNull(identity.tokenKey()) || identity.tokenKey().isBlank()) {
            log.warn("NEXO_TOKEN_KEY vazia: usando uma chave aleatória. Os refresh tokens não decifram depois de reiniciar (só para desenvolvimento).");

            KeyGenerator generator = KeyGenerator.getInstance("AES");

            generator.init(KEY_BYTES * 8);
            keys.put(identity.tokenKeyVersion(), generator.generateKey());
        } else {
            keys.put(identity.tokenKeyVersion(), this.key(identity.tokenKey(), "NEXO_TOKEN_KEY"));
        }

        if (nonNull(identity.previousTokenKeys())) {
            identity.previousTokenKeys().forEach((version, key) -> keys.putIfAbsent(version, this.key(key, "NEXO_TOKEN_KEY_V" + version)));
        }

        return new AesGcmTokenCipher(keys, identity.tokenKeyVersion());
    }

    @Bean
    GoogleOAuthPort googleOAuth(
            ClientRegistrationRepository registrations,
            IdentityProperties identity,
            ObjectMapper json
    ) {
        return new GoogleOAuthAdapter(
                registrations,
                identity.googleTokenUri(),
                identity.googleRevokeUri(),
                json
        );
    }

    private SecretKey key(
            String base64,
            String name
    ) {
        byte[] bytes = Base64.getDecoder().decode(base64.strip());

        if (bytes.length != KEY_BYTES) {
            throw new IllegalArgumentException(name + " precisa ter 32 bytes em Base64 (gere com: openssl rand -base64 32); veio " + bytes.length);
        }

        return new SecretKeySpec(bytes, "AES");
    }
}
