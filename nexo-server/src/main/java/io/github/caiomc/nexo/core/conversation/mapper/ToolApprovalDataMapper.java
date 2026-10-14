package io.github.caiomc.nexo.core.conversation.mapper;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.conversation.domain.enums.ApprovalStatus;
import io.github.caiomc.nexo.core.conversation.domain.enums.Severity;
import io.github.caiomc.nexo.core.conversation.domain.model.ToolApproval;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.port.model.data.FunctionCallData;
import io.github.caiomc.nexo.core.conversation.port.repository.data.ToolApprovalData;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

/**
 * {@link ToolApprovalData} ⇄ {@link ToolApproval}.
 */
@Component
public class ToolApprovalDataMapper {

    public ToolApproval toApproval(ToolApprovalData data) {
        return new ToolApproval(
                data.id(),
                UserId.of(data.userId()),
                data.turnId(),
                data.callId(),
                ToolName.of(data.toolName()),
                data.description(),
                Severity.fromWire(data.severity()),
                data.arguments(),
                data.argumentsHash(),
                ApprovalStatus.valueOf(data.status()),
                data.createdAt(),
                data.expiresAt(),
                data.decidedAt()
        );
    }

    public ToolApprovalData toData(
            ToolApproval approval,
            Turn turn
    ) {
        return new ToolApprovalData(
                approval.id(),
                approval.userId().value(),
                turn.conversationId(),
                approval.turnId(),
                approval.callId(),
                approval.tool().value(),
                approval.description(),
                approval.severity().wire(),
                approval.arguments(),
                approval.argumentsHash(),
                approval.status().name(),
                approval.createdAt(),
                approval.expiresAt(),
                approval.decidedAt()
        );
    }

    public ToolApproval pending(
            Turn turn,
            FunctionCallData call,
            Tool tool,
            String description,
            String argumentsHash,
            Instant now,
            Duration ttl
    ) {
        return ToolApproval.pending(
                turn.userId(),
                turn.id(),
                call.callId(),
                tool.name(),
                description,
                Severity.of(tool.effect()),
                call.arguments(),
                argumentsHash,
                now,
                ttl
        );
    }
}
