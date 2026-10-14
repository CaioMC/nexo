package io.github.caiomc.nexo.core.identity.port.cache;

import java.util.Optional;

import io.github.caiomc.nexo.core.identity.domain.model.AccessToken;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

/**
 * Access tokens só em memória, por usuário.
 */
public interface AccessTokenCache {

    Optional<AccessToken> get(UserId userId);

    void put(
            UserId userId,
            AccessToken token
    );

    void evict(UserId userId);
}
