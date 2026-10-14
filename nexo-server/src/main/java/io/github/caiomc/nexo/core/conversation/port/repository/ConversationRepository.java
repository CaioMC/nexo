package io.github.caiomc.nexo.core.conversation.port.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.caiomc.nexo.core.conversation.port.repository.data.ConversationData;

public interface ConversationRepository {

    Optional<ConversationData> findById(UUID id);

    /**
     * Mais recentes primeiro.
     *
     * @param before só as atualizadas antes deste instante; nulo = sem corte
     */
    List<ConversationData> findByUser(
            UUID userId,
            Instant before,
            int limit
    );

    /**
     * Grava ou atualiza título e {@code updated_at} ({@code last_seq} é do Event Store).
     */
    void save(ConversationData conversation);

    void delete(UUID id);
}
