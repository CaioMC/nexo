package io.github.caiomc.nexo.core.conversation.port.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.caiomc.nexo.core.conversation.port.repository.data.ToolApprovalData;

public interface ApprovalRepository {

    void insert(ToolApprovalData approval);

    Optional<ToolApprovalData> findById(UUID id);

    /**
     * {@code PENDING} → {@code to}, atômico ({@code UPDATE ... WHERE status = 'PENDING'}): entre
     * duas decisões simultâneas, só uma vence.
     *
     * @return {@code false} se a aprovação não estava mais pendente
     */
    boolean resolvePending(
            UUID id,
            String to,
            Instant decidedAt
    );

    /**
     * {@code APPROVED → CONSUMED} só se o hash for o dos argumentos aprovados (RN-05).
     */
    boolean consume(
            UUID id,
            String argumentsHash
    );

    /**
     * @param status nulo = todas
     */
    List<ToolApprovalData> findByConversation(
            UUID conversationId,
            String status
    );

    List<ToolApprovalData> findPendingExpiredAt(Instant now);

    List<ToolApprovalData> findPendingByTurn(UUID turnId);

    List<ToolApprovalData> findAllPending();
}
