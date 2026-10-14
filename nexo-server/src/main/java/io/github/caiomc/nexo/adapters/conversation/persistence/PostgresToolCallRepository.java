package io.github.caiomc.nexo.adapters.conversation.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.adapters.shared.persistence.JsonColumns;
import io.github.caiomc.nexo.adapters.shared.persistence.SqlArrays;
import io.github.caiomc.nexo.core.conversation.port.repository.ToolCallRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.data.ExecutedToolData;
import io.github.caiomc.nexo.core.conversation.port.repository.data.ToolCallData;

import static java.util.Objects.isNull;

@Component
public class PostgresToolCallRepository implements ToolCallRepository {

    private final JdbcClient jdbc;
    private final JsonColumns json;

    public PostgresToolCallRepository(
            JdbcClient jdbc,
            JsonColumns json
    ) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public void save(ToolCallData call) {
        this.jdbc.sql("""
                        INSERT INTO tool_call (turn_id, call_id, tool_name, arguments, status, block_reason, missing, output_summary, error, started_at, finished_at)
                        VALUES (:turn, :call, :tool, CAST(:arguments AS jsonb), :status, :reason, string_to_array(:missing, ','), :summary, :error, :startedAt, :finishedAt)
                        ON CONFLICT (turn_id, call_id) DO UPDATE
                           SET status = EXCLUDED.status,
                               output_summary = EXCLUDED.output_summary,
                               error = EXCLUDED.error,
                               finished_at = EXCLUDED.finished_at
                        """)
                .param("turn", call.turnId())
                .param("call", call.callId())
                .param("tool", call.toolName())
                .param("arguments", this.json.write(call.arguments()))
                .param("status", call.status())
                .param("reason", call.blockReason())
                .param("missing", String.join(",", call.missing()))
                .param("summary", call.outputSummary())
                .param("error", call.error())
                .param("startedAt", Timestamp.from(call.startedAt()))
                .param("finishedAt", this.timestamp(call.finishedAt()))
                .update();
    }

    @Override
    public List<ExecutedToolData> succeededSince(
            UUID conversationId,
            Instant since
    ) {
        return this.jdbc.sql("""
                        SELECT c.tool_name, c.finished_at
                          FROM tool_call c
                          JOIN turn t ON t.id = c.turn_id
                         WHERE t.conversation_id = :conversation
                           AND c.status = 'SUCCEEDED'
                           AND c.finished_at >= :since
                        """)
                .param("conversation", conversationId)
                .param("since", Timestamp.from(since))
                .query((row, index) -> new ExecutedToolData(row.getString("tool_name"), row.getTimestamp("finished_at").toInstant()))
                .list();
    }

    @Override
    public List<ToolCallData> findByTurns(Collection<UUID> turnIds) {
        if (turnIds.isEmpty()) {
            return List.of();
        }

        return this.jdbc.sql("""
                        SELECT turn_id, call_id, tool_name, arguments::text AS arguments, status, block_reason, missing, output_summary, error, started_at, finished_at
                          FROM tool_call
                         WHERE turn_id = ANY (string_to_array(:turns, ',')::uuid[])
                         ORDER BY started_at
                        """)
                .param("turns", turnIds.stream().map(UUID::toString).collect(Collectors.joining(",")))
                .query(this::toData)
                .list();
    }

    private Timestamp timestamp(Instant instant) {
        return isNull(instant) ? null : Timestamp.from(instant);
    }

    private ToolCallData toData(
            ResultSet row,
            int index
    ) throws SQLException {
        Timestamp finished = row.getTimestamp("finished_at");

        return new ToolCallData(
                row.getObject("turn_id", UUID.class),
                row.getString("call_id"),
                row.getString("tool_name"),
                this.json.read(row.getString("arguments")),
                row.getString("status"),
                row.getString("block_reason"),
                SqlArrays.strings(row.getArray("missing")),
                row.getString("output_summary"),
                row.getString("error"),
                row.getTimestamp("started_at").toInstant(),
                isNull(finished) ? null : finished.toInstant()
        );
    }
}
