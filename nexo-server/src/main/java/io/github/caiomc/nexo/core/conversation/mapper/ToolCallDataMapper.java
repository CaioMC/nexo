package io.github.caiomc.nexo.core.conversation.mapper;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.conversation.domain.enums.BlockReason;
import io.github.caiomc.nexo.core.conversation.domain.enums.ToolCallStatus;
import io.github.caiomc.nexo.core.conversation.domain.model.ToolCall;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.port.model.data.FunctionCallData;
import io.github.caiomc.nexo.core.conversation.port.repository.data.ToolCallData;

import static java.util.Objects.isNull;

/**
 * {@link ToolCallData} ⇄ {@link ToolCall}.
 */
@Component
public class ToolCallDataMapper {

    public ToolCall toCall(ToolCallData data) {
        return new ToolCall(
                data.turnId(),
                data.callId(),
                ToolName.of(data.toolName()),
                data.arguments(),
                ToolCallStatus.valueOf(data.status()),
                isNull(data.blockReason()) ? null : BlockReason.valueOf(data.blockReason()),
                data.missing(),
                data.outputSummary(),
                data.error(),
                data.startedAt(),
                data.finishedAt()
        );
    }

    public ToolCallData toData(ToolCall call) {
        return new ToolCallData(
                call.turnId(),
                call.callId(),
                call.tool().value(),
                call.arguments(),
                call.status().name(),
                isNull(call.blockReason()) ? null : call.blockReason().name(),
                call.missing(),
                call.outputSummary(),
                call.error(),
                call.startedAt(),
                call.finishedAt()
        );
    }

    public ToolCall proposed(
            Turn turn,
            FunctionCallData call,
            Tool tool,
            ToolCallStatus status,
            Instant now
    ) {
        return ToolCall.proposed(
                turn.id(),
                call.callId(),
                tool.name(),
                call.arguments(),
                status,
                now
        );
    }

    public ToolCall blocked(
            Turn turn,
            FunctionCallData call,
            Tool tool,
            BlockReason reason,
            List<String> missing,
            Instant now
    ) {
        return ToolCall.blocked(
                turn.id(),
                call.callId(),
                tool.name(),
                call.arguments(),
                reason,
                missing,
                now
        );
    }
}
