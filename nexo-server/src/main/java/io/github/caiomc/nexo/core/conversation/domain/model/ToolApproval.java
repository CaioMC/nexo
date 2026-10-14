package io.github.caiomc.nexo.core.conversation.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalStatus;
import io.github.caiomc.nexo.core.conversation.domain.enums.Severity;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

/**
 * Pedido de confirmação de uma alteração (RN-05): de uso único, para exatamente os argumentos
 * mostrados (hash), com validade (RN-06).
 *
 * @param description gerada pelo servidor a partir dos argumentos, nunca pelo texto do modelo
 */
public record ToolApproval(
        UUID id,
        UserId userId,
        UUID turnId,
        String callId,
        ToolName tool,
        String description,
        Severity severity,
        Map<String, Object> arguments,
        String argumentsHash,
        ApprovalStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant decidedAt
) {

    public ToolApproval {
        if (isNull(id) || isNull(userId) || isNull(turnId) || isNull(callId) || isNull(tool)) {
            throw new IllegalArgumentException("Aprovação precisa de id, usuário, turno, chamada e ferramenta");
        }

        if (isNull(argumentsHash) || argumentsHash.isBlank()) {
            throw new IllegalArgumentException("Aprovação sem hash dos argumentos");
        }

        arguments = isNull(arguments) ? Map.of() : arguments;
    }

    public static ToolApproval pending(
            UserId userId,
            UUID turnId,
            String callId,
            ToolName tool,
            String description,
            Severity severity,
            Map<String, Object> arguments,
            String argumentsHash,
            Instant now,
            Duration ttl
    ) {
        return new ToolApproval(
                UUID.randomUUID(),
                userId,
                turnId,
                callId,
                tool,
                description,
                severity,
                arguments,
                argumentsHash,
                ApprovalStatus.PENDING,
                now,
                now.plus(ttl),
                null
        );
    }

    public boolean expiredAt(Instant now) {
        return !now.isBefore(this.expiresAt);
    }

    public boolean belongsTo(UserId user) {
        return this.userId.equals(user);
    }
}
