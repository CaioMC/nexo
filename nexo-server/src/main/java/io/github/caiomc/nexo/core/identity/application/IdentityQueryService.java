package io.github.caiomc.nexo.core.identity.application;

import java.util.Set;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.identity.domain.exception.UserNotFoundException;
import io.github.caiomc.nexo.core.identity.domain.model.User;
import io.github.caiomc.nexo.core.identity.mapper.GoogleCredentialDataMapper;
import io.github.caiomc.nexo.core.identity.mapper.UserDataMapper;
import io.github.caiomc.nexo.core.identity.port.repository.GoogleCredentialRepository;
import io.github.caiomc.nexo.core.identity.port.repository.UserRepository;
import io.github.caiomc.nexo.core.identity.usecase.GetCurrentUserUseCase;
import io.github.caiomc.nexo.core.identity.usecase.GetGrantedScopesUseCase;
import io.github.caiomc.nexo.core.identity.usecase.command.GetCurrentUserCommand;
import io.github.caiomc.nexo.core.identity.usecase.command.GetGrantedScopesCommand;

/**
 * Consultas da identidade: o usuário logado (RF-05) e os escopos já concedidos.
 */
@Service
public class IdentityQueryService implements GetCurrentUserUseCase, GetGrantedScopesUseCase {

    private final UserRepository users;
    private final GoogleCredentialRepository credentials;

    private final UserDataMapper userMapper;
    private final GoogleCredentialDataMapper credentialMapper;

    public IdentityQueryService(
            UserRepository users,
            GoogleCredentialRepository credentials,
            UserDataMapper userMapper,
            GoogleCredentialDataMapper credentialMapper
    ) {
        this.users = users;
        this.credentials = credentials;
        this.userMapper = userMapper;
        this.credentialMapper = credentialMapper;
    }

    @Override
    public User get(GetCurrentUserCommand command) {
        return this.users.findById(command.userId().value())
                .map(this.userMapper::toUser)
                .orElseThrow(() -> new UserNotFoundException(command.userId()));
    }

    @Override
    public Set<String> granted(GetGrantedScopesCommand command) {
        return this.credentials.findByUser(command.userId().value())
                .map(this.credentialMapper::toCredential)
                .map(credential -> credential.scopes().values())
                .orElse(Set.of());
    }
}
