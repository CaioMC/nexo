package io.github.caiomc.nexo.core.identity.port.repository;

import java.util.Optional;
import java.util.UUID;

import io.github.caiomc.nexo.core.identity.port.repository.data.UserData;

public interface UserRepository {

    Optional<UserData> findById(UUID id);

    Optional<UserData> findBySubject(String googleSubject);

    void save(UserData user);
}
