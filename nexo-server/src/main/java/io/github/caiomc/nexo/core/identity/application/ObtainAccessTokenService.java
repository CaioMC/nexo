package io.github.caiomc.nexo.core.identity.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.identity.domain.exception.ConsentRevokedException;
import io.github.caiomc.nexo.core.identity.domain.exception.GoogleCredentialMissingException;
import io.github.caiomc.nexo.core.identity.domain.model.AccessToken;
import io.github.caiomc.nexo.core.identity.domain.model.GoogleCredential;
import io.github.caiomc.nexo.core.identity.mapper.GoogleCredentialDataMapper;
import io.github.caiomc.nexo.core.identity.mapper.IdentityCommandMapper;
import io.github.caiomc.nexo.core.identity.port.cache.AccessTokenCache;
import io.github.caiomc.nexo.core.identity.port.crypto.TokenCipherPort;
import io.github.caiomc.nexo.core.identity.port.oauth.GoogleOAuthPort;
import io.github.caiomc.nexo.core.identity.port.repository.GoogleCredentialRepository;
import io.github.caiomc.nexo.core.identity.usecase.ObtainAccessTokenUseCase;
import io.github.caiomc.nexo.core.identity.usecase.command.ObtainAccessTokenCommand;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.workspace.usecase.ReconcileContextsUseCase;

/**
 * Access token para chamar o Google em nome do usuário (doc 04, seção 5): do cache se ainda vale
 * por mais de 60 s; senão, refresh com o token guardado.
 */
@Service
public class ObtainAccessTokenService implements ObtainAccessTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(ObtainAccessTokenService.class);

    private static final Duration MARGIN = Duration.ofSeconds(60);

    private final AccessTokenCache cache;
    private final GoogleCredentialRepository credentials;
    private final TokenCipherPort cipher;
    private final GoogleOAuthPort oauth;
    private final ReconcileContextsUseCase contexts;

    private final GoogleCredentialDataMapper mapper;
    private final IdentityCommandMapper commands;
    private final Clock clock;

    public ObtainAccessTokenService(
            AccessTokenCache cache,
            GoogleCredentialRepository credentials,
            TokenCipherPort cipher,
            GoogleOAuthPort oauth,
            ReconcileContextsUseCase contexts,
            GoogleCredentialDataMapper mapper,
            IdentityCommandMapper commands,
            Clock clock
    ) {
        this.cache = cache;
        this.credentials = credentials;
        this.cipher = cipher;
        this.oauth = oauth;
        this.contexts = contexts;
        this.mapper = mapper;
        this.commands = commands;
        this.clock = clock;
    }

    @Override
    public AccessToken obtain(ObtainAccessTokenCommand command) {
        UserId userId = command.userId();
        Instant now = this.clock.instant();
        Optional<AccessToken> cached = this.cache.get(userId);

        if (cached.isPresent() && cached.get().validFor(now, MARGIN)) {
            return cached.get();
        }

        GoogleCredential credential = this.credentials.findByUser(userId.value())
                .map(this.mapper::toCredential)
                .orElseThrow(() -> new GoogleCredentialMissingException(userId));
        String refreshToken = this.cipher.decrypt(credential.refreshToken(), userId);
        AccessToken token;

        try {
            token = this.mapper.toAccessToken(this.oauth.refresh(this.mapper.toRefreshCommand(userId, refreshToken)), now);
        } catch (ConsentRevokedException e) {
            this.forget(userId);
            throw e;
        }

        this.store(
                credential,
                refreshToken,
                token,
                now
        );
        this.cache.put(userId, token);

        return token;
    }

    @Override
    public void invalidate(ObtainAccessTokenCommand command) {
        this.cache.evict(command.userId());
    }

    /**
     * Atualiza os escopos (o refresh devolve a soma concedida) e recifra se a chave mudou. Escopo
     * que sumiu desliga os contextos que dependiam dele (RN-01).
     */
    private void store(
            GoogleCredential credential,
            String refreshToken,
            AccessToken token,
            Instant now
    ) {
        GoogleCredential updated = token.scopes().values().isEmpty() ? credential : credential.withScopes(token.scopes(), now);

        if (updated.refreshToken().keyVersion() != this.cipher.currentKeyVersion()) {
            updated = updated.reencrypted(this.cipher.encrypt(refreshToken, credential.userId()), now);
        }

        if (!updated.equals(credential)) {
            this.credentials.save(this.mapper.toData(updated));
        }

        if (!updated.scopes().equals(credential.scopes())) {
            this.contexts.reconcile(this.commands.toReconcile(credential.userId(), updated.scopes().values()));
        }
    }

    private void forget(UserId userId) {
        log.warn("Refresh token de {} recusado pelo Google: credencial apagada e contextos desligados", userId);
        this.credentials.delete(userId.value());
        this.cache.evict(userId);
        this.contexts.reconcile(this.commands.toRevoked(userId));
    }
}
