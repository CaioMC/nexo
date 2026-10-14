package io.github.caiomc.nexo.core.identity.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.identity.mapper.GoogleCredentialDataMapper;
import io.github.caiomc.nexo.core.identity.mapper.IdentityCommandMapper;
import io.github.caiomc.nexo.core.identity.port.cache.AccessTokenCache;
import io.github.caiomc.nexo.core.identity.port.crypto.TokenCipherPort;
import io.github.caiomc.nexo.core.identity.port.oauth.GoogleOAuthPort;
import io.github.caiomc.nexo.core.identity.port.repository.GoogleCredentialRepository;
import io.github.caiomc.nexo.core.identity.usecase.DisconnectGoogleUseCase;
import io.github.caiomc.nexo.core.identity.usecase.command.DisconnectGoogleCommand;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;
import io.github.caiomc.nexo.core.workspace.usecase.ReconcileContextsUseCase;

/**
 * Desconectar a conta (HU-06): revoga o token no Google, apaga a credencial e desliga todos os
 * contextos. Invalidar a sessão é com o adapter web.
 */
@Service
public class DisconnectGoogleService implements DisconnectGoogleUseCase {

    private static final Logger log = LoggerFactory.getLogger(DisconnectGoogleService.class);

    private final GoogleCredentialRepository credentials;
    private final TokenCipherPort cipher;
    private final GoogleOAuthPort oauth;
    private final AccessTokenCache cache;
    private final ReconcileContextsUseCase contexts;

    private final GoogleCredentialDataMapper mapper;
    private final IdentityCommandMapper commands;

    public DisconnectGoogleService(
            GoogleCredentialRepository credentials,
            TokenCipherPort cipher,
            GoogleOAuthPort oauth,
            AccessTokenCache cache,
            ReconcileContextsUseCase contexts,
            GoogleCredentialDataMapper mapper,
            IdentityCommandMapper commands
    ) {
        this.credentials = credentials;
        this.cipher = cipher;
        this.oauth = oauth;
        this.cache = cache;
        this.contexts = contexts;
        this.mapper = mapper;
        this.commands = commands;
    }

    @Override
    public void disconnect(DisconnectGoogleCommand command) {
        UserId userId = command.userId();

        this.credentials.findByUser(userId.value())
                .map(this.mapper::toCredential)
                .map(credential -> this.cipher.decrypt(credential.refreshToken(), userId))
                .ifPresent(token -> this.oauth.revoke(this.mapper.toRevokeCommand(token)));

        this.credentials.delete(userId.value());
        this.cache.evict(userId);
        this.contexts.reconcile(this.commands.toRevoked(userId));
        log.info("Conta Google de {} desconectada", userId);
    }
}
