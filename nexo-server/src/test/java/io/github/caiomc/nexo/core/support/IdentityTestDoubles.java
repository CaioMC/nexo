package io.github.caiomc.nexo.core.support;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.github.caiomc.nexo.core.identity.domain.exception.ConsentRevokedException;
import io.github.caiomc.nexo.core.identity.domain.model.AccessToken;
import io.github.caiomc.nexo.core.identity.domain.model.EncryptedToken;
import io.github.caiomc.nexo.core.identity.port.cache.AccessTokenCache;
import io.github.caiomc.nexo.core.identity.port.crypto.TokenCipherPort;
import io.github.caiomc.nexo.core.identity.port.oauth.GoogleOAuthPort;
import io.github.caiomc.nexo.core.identity.port.oauth.command.RefreshTokenCommand;
import io.github.caiomc.nexo.core.identity.port.oauth.command.RevokeTokenCommand;
import io.github.caiomc.nexo.core.identity.port.oauth.data.TokenData;
import io.github.caiomc.nexo.core.identity.port.repository.GoogleCredentialRepository;
import io.github.caiomc.nexo.core.identity.port.repository.data.GoogleCredentialData;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.workspace.usecase.ReconcileContextsUseCase;
import io.github.caiomc.nexo.core.workspace.usecase.command.ReconcileContextsCommand;

/**
 * Dublês do contexto de identidade.
 */
public final class IdentityTestDoubles {

    private IdentityTestDoubles() {
    }

    /**
     * "Cifra" reversível e visível, só para teste: {@code dono:token}.
     */
    public static final class PlainCipher implements TokenCipherPort {

        public int version = 1;

        @Override
        public EncryptedToken encrypt(
                String token,
                UserId owner
        ) {
            return new EncryptedToken((owner + ":" + token).getBytes(StandardCharsets.UTF_8), this.version);
        }

        @Override
        public String decrypt(
                EncryptedToken token,
                UserId owner
        ) {
            return new String(token.bytes(), StandardCharsets.UTF_8).substring(owner.toString().length() + 1);
        }

        @Override
        public int currentKeyVersion() {
            return this.version;
        }
    }

    public static final class ScriptedOAuth implements GoogleOAuthPort {

        public final List<String> refreshed = new ArrayList<>();
        public final List<String> revoked = new ArrayList<>();
        public Set<String> scopes = Set.of("https://www.googleapis.com/auth/calendar");
        public boolean revokedByGoogle;

        @Override
        public TokenData refresh(RefreshTokenCommand command) {
            this.refreshed.add(command.refreshToken());

            if (this.revokedByGoogle) {
                throw new ConsentRevokedException(command.userId());
            }

            return new TokenData("access-" + this.refreshed.size(), 3600, this.scopes);
        }

        @Override
        public void revoke(RevokeTokenCommand command) {
            this.revoked.add(command.token());
        }
    }

    public static final class InMemoryCredentials implements GoogleCredentialRepository {

        public final Map<UUID, GoogleCredentialData> rows = new HashMap<>();

        @Override
        public Optional<GoogleCredentialData> findByUser(UUID userId) {
            return Optional.ofNullable(this.rows.get(userId));
        }

        @Override
        public void save(GoogleCredentialData credential) {
            this.rows.put(credential.userId(), credential);
        }

        @Override
        public void delete(UUID userId) {
            this.rows.remove(userId);
        }
    }

    public static final class MapTokenCache implements AccessTokenCache {

        public final Map<UserId, AccessToken> tokens = new HashMap<>();

        @Override
        public Optional<AccessToken> get(UserId userId) {
            return Optional.ofNullable(this.tokens.get(userId));
        }

        @Override
        public void put(
                UserId userId,
                AccessToken token
        ) {
            this.tokens.put(userId, token);
        }

        @Override
        public void evict(UserId userId) {
            this.tokens.remove(userId);
        }
    }

    public static final class RecordingReconcile implements ReconcileContextsUseCase {

        public final List<ReconcileContextsCommand> received = new ArrayList<>();

        @Override
        public void reconcile(ReconcileContextsCommand command) {
            this.received.add(command);
        }
    }
}
