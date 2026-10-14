package io.github.caiomc.nexo.core.identity.port.repository;

import java.util.Optional;
import java.util.UUID;

import io.github.caiomc.nexo.core.identity.port.repository.data.GoogleCredentialData;

public interface GoogleCredentialRepository {

    Optional<GoogleCredentialData> findByUser(UUID userId);

    void save(GoogleCredentialData credential);

    void delete(UUID userId);
}
