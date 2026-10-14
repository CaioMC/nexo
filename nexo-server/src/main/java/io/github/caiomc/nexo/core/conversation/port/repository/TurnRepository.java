package io.github.caiomc.nexo.core.conversation.port.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.caiomc.nexo.core.conversation.domain.exception.TurnInProgressException;
import io.github.caiomc.nexo.core.conversation.port.repository.data.TurnData;

public interface TurnRepository {

    Optional<TurnData> findById(UUID id);

    Optional<TurnData> findActive(UUID conversationId);

    Optional<TurnData> findByClientMessage(
            UUID conversationId,
            UUID clientMessageId
    );

    /**
     * Turnos terminados, mais recentes primeiro.
     */
    List<TurnData> findFinished(
            UUID conversationId,
            int limit
    );

    /**
     * Todos os turnos {@code RUNNING} ou {@code WAITING_APPROVAL} (recuperação na subida).
     */
    List<TurnData> findAllActive();

    /**
     * @throws TurnInProgressException se a conversa já tem um turno ativo (índice único)
     */
    void insert(TurnData turn);

    void update(TurnData turn);
}
