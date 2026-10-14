package io.github.caiomc.nexo.core.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.identity.domain.model.GoogleCredential;
import io.github.caiomc.nexo.core.identity.domain.model.GrantedScopes;
import io.github.caiomc.nexo.core.identity.domain.model.SignIn;
import io.github.caiomc.nexo.core.identity.domain.model.User;
import io.github.caiomc.nexo.core.identity.mapper.GoogleCredentialDataMapper;
import io.github.caiomc.nexo.core.identity.mapper.IdentityCommandMapper;
import io.github.caiomc.nexo.core.identity.mapper.UserDataMapper;
import io.github.caiomc.nexo.core.identity.port.cache.AccessTokenCache;
import io.github.caiomc.nexo.core.identity.port.crypto.TokenCipherPort;
import io.github.caiomc.nexo.core.identity.port.repository.GoogleCredentialRepository;
import io.github.caiomc.nexo.core.identity.port.repository.UserRepository;
import io.github.caiomc.nexo.core.identity.usecase.SignInWithGoogleUseCase;
import io.github.caiomc.nexo.core.identity.usecase.command.SignInCommand;
import io.github.caiomc.nexo.core.workspace.usecase.ReconcileContextsUseCase;

import static java.util.Objects.nonNull;

/**
 * Login com Google (doc 04, seção 3): cria ou atualiza o usuário pelo {@code sub}, guarda o refresh
 * token cifrado e os escopos, e põe o access token no cache em memória.
 */
@Service
public class SignInWithGoogleService implements SignInWithGoogleUseCase {

    private static final Logger log = LoggerFactory.getLogger(SignInWithGoogleService.class);

    private final UserRepository users;
    private final GoogleCredentialRepository credentials;
    private final TokenCipherPort cipher;
    private final AccessTokenCache cache;
    private final ReconcileContextsUseCase contexts;

    private final UserDataMapper userMapper;
    private final GoogleCredentialDataMapper credentialMapper;
    private final IdentityCommandMapper commands;
    private final Clock clock;

    public SignInWithGoogleService(
            UserRepository users,
            GoogleCredentialRepository credentials,
            TokenCipherPort cipher,
            AccessTokenCache cache,
            ReconcileContextsUseCase contexts,
            UserDataMapper userMapper,
            GoogleCredentialDataMapper credentialMapper,
            IdentityCommandMapper commands,
            Clock clock
    ) {
        this.users = users;
        this.credentials = credentials;
        this.cipher = cipher;
        this.cache = cache;
        this.contexts = contexts;
        this.userMapper = userMapper;
        this.credentialMapper = credentialMapper;
        this.commands = commands;
        this.clock = clock;
    }

    @Override
    public SignIn signIn(SignInCommand command) {
        Instant now = this.clock.instant();
        User user = this.users.findBySubject(command.googleSubject())
                .map(this.userMapper::toUser)
                .map(existing -> this.userMapper.loggedIn(existing, command, now))
                .orElseGet(() -> this.userMapper.firstLogin(command, now));

        this.users.save(this.userMapper.toData(user));

        Optional<GoogleCredential> credential = this.saveCredential(user, command, now);

        if (nonNull(command.accessToken()) && nonNull(command.accessTokenExpiresAt())) {
            this.cache.put(user.id(), this.credentialMapper.toAccessToken(command));
        }

        this.contexts.reconcile(this.commands.toReconcile(user.id(), command.grantedScopes()));
        log.info("Login de {} ({} escopos concedidos)", user.id(), command.grantedScopes().size());

        return this.userMapper.toSignIn(user, credential.isPresent());
    }

    /**
     * Refresh token novo substitui o anterior; sem um novo, só os escopos são atualizados.
     */
    private Optional<GoogleCredential> saveCredential(
            User user,
            SignInCommand command,
            Instant now
    ) {
        Optional<GoogleCredential> stored = this.credentials.findByUser(user.id().value()).map(this.credentialMapper::toCredential);
        Optional<GoogleCredential> updated;

        if (nonNull(command.refreshToken()) && !command.refreshToken().isBlank()) {
            updated = Optional.of(
                    this.credentialMapper.newCredential(
                            user.id(),
                            this.cipher.encrypt(command.refreshToken(), user.id()),
                            command.grantedScopes(),
                            now
                    )
            );
        } else {
            updated = stored.map(credential -> credential.withScopes(GrantedScopes.of(command.grantedScopes()), now));
        }

        updated.ifPresent(credential -> this.credentials.save(this.credentialMapper.toData(credential)));

        return updated;
    }
}
