package io.github.caiomc.nexo.adapters.identity.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.identity.domain.model.AccessToken;
import io.github.caiomc.nexo.core.identity.port.cache.AccessTokenCache;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

/**
 * Access tokens só na memória do processo: reiniciar o servidor custa um refresh por usuário.
 */
@Component
public class InMemoryAccessTokenCache implements AccessTokenCache {

    private final Map<UserId, AccessToken> tokens = new ConcurrentHashMap<>();

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
