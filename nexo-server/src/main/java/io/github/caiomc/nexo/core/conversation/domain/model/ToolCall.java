package io.github.caiomc.nexo.core.conversation.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.conversation.domain.enums.BlockReason;
import io.github.caiomc.nexo.core.conversation.domain.enums.ToolCallStatus;

import static java.util.Objects.isNull;

/**
 * Uma chamada de ferramenta proposta pelo modelo e o que aconteceu com ela.
 */
public record ToolCall(
        UUID turnId,
        String callId,
        ToolName tool,
        Map<String, Object> arguments,
        ToolCallStatus status,
        BlockReason blockReason,
        List<String> missing,
        String outputSummary,
        String error,
        Instant startedAt,
        Instant finishedAt
) {

    public ToolCall {
        if (isNull(turnId) || isNull(callId) || isNull(tool) || isNull(status)) {
            throw new IllegalArgumentException("Chamada de ferramenta precisa de turno, callId, ferramenta e estado");
        }

        arguments = isNull(arguments) ? Map.of() : arguments;
        missing = isNull(missing) ? List.of() : List.copyOf(missing);
    }

    public static ToolCall blocked(
            UUID turnId,
            String callId,
            ToolName tool,
            Map<String, Object> arguments,
            BlockReason reason,
            List<String> missing,
            Instant now
    ) {
        return new ToolCall(
                turnId,
                callId,
                tool,
                arguments,
                ToolCallStatus.BLOCKED,
                reason,
                missing,
                null,
                null,
                now,
                now
        );
    }

    public static ToolCall proposed(
            UUID turnId,
            String callId,
            ToolName tool,
            Map<String, Object> arguments,
            ToolCallStatus status,
            Instant now
    ) {
        return new ToolCall(
                turnId,
                callId,
                tool,
                arguments,
                status,
                null,
                List.of(),
                null,
                null,
                now,
                null
        );
    }

    public ToolCall running() {
        return this.finish(
                ToolCallStatus.RUNNING,
                null,
                null,
                null
        );
    }

    public ToolCall succeeded(
            String summary,
            Instant now
    ) {
        return this.finish(
                ToolCallStatus.SUCCEEDED,
                summary,
                null,
                now
        );
    }

    public ToolCall failed(
            String message,
            Instant now
    ) {
        return this.finish(
                ToolCallStatus.FAILED,
                null,
                message,
                now
        );
    }

    public ToolCall rejected(Instant now) {
        return this.finish(
                ToolCallStatus.REJECTED,
                null,
                null,
                now
        );
    }

    private ToolCall finish(
            ToolCallStatus next,
            String summary,
            String message,
            Instant now
    ) {
        return new ToolCall(
                this.turnId,
                this.callId,
                this.tool,
                this.arguments,
                next,
                this.blockReason,
                this.missing,
                summary,
                message,
                this.startedAt,
                now
        );
    }
}
