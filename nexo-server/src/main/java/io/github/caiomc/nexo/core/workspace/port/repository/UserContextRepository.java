package io.github.caiomc.nexo.core.workspace.port.repository;

import java.util.List;
import java.util.UUID;

import io.github.caiomc.nexo.core.workspace.port.repository.data.UserContextData;

public interface UserContextRepository {

    List<UserContextData> findByUser(UUID userId);

    /**
     * Grava ou substitui o estado do contexto para o usuário.
     */
    void save(UserContextData context);
}
