package io.github.caiomc.nexo.adapters.conversation.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.adapters.shared.persistence.SqlArrays;
import io.github.caiomc.nexo.core.conversation.domain.exception.TurnInProgressException;
import io.github.caiomc.nexo.core.conversation.port.repository.TurnRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.data.TurnData;

import static java.util.Objects.isNull;

/**
 * {@code turn}. O índice único parcial {@code turn_one_active_idx} garante um turno ativo por
 * conversa mesmo com duas abas mandando mensagem ao mesmo tempo (RF-25).
 */
@Component
public class PostgresTurnRepository implements TurnRepository {

    private static final String SELECT = """
            SELECT t.id, t.conversation_id, c.user_id, t.client_message_id, t.user_message, t.assistant_message, t.status,
                   t.failure_code, t.model, t.enabled_contexts, t.model_calls, t.input_tokens, t.cached_input_tokens,
                   t.output_tokens, t.started_at, t.finished_at
              FROM turn t
              JOIN conversation c ON c.id = t.conversation_id
            """;

    private final JdbcClient jdbc;

    public PostgresTurnRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<TurnData> findById(UUID id) {
        return this.jdbc.sql(SELECT + " WHERE t.id = :id").param("id", id).query(this::toData).optional();
    }

    @Override
    public Optional<TurnData> findActive(UUID conversationId) {
        return this.jdbc.sql(SELECT + " WHERE t.conversation_id = :conversation AND t.status IN ('RUNNING', 'WAITING_APPROVAL')")
                .param("conversation", conversationId)
                .query(this::toData)
                .optional();
    }

    @Override
    public Optional<TurnData> findByClientMessage(
            UUID conversationId,
            UUID clientMessageId
    ) {
        return this.jdbc.sql(SELECT + " WHERE t.conversation_id = :conversation AND t.client_message_id = :client")
                .param("conversation", conversationId)
                .param("client", clientMessageId)
                .query(this::toData)
                .optional();
    }

    @Override
    public List<TurnData> findFinished(
            UUID conversationId,
            int limit
    ) {
        return this.jdbc.sql(SELECT + " WHERE t.conversation_id = :conversation AND t.status IN ('COMPLETED', 'FAILED') ORDER BY t.started_at DESC LIMIT :limit")
                .param("conversation", conversationId)
                .param("limit", limit)
                .query(this::toData)
                .list();
    }

    @Override
    public List<TurnData> findAllActive() {
        return this.jdbc.sql(SELECT + " WHERE t.status IN ('RUNNING', 'WAITING_APPROVAL')").query(this::toData).list();
    }

    @Override
    public void insert(TurnData turn) {
        try {
            this.jdbc.sql("""
                            INSERT INTO turn (id, conversation_id, client_message_id, user_message, status, model, enabled_contexts, started_at)
                            VALUES (:id, :conversation, :client, :message, :status, :model, string_to_array(:contexts, ','), :startedAt)
                            """)
                    .param("id", turn.id())
                    .param("conversation", turn.conversationId())
                    .param("client", turn.clientMessageId())
                    .param("message", turn.userMessage())
                    .param("status", turn.status())
                    .param("model", turn.model())
                    .param("contexts", String.join(",", turn.enabledContexts()))
                    .param("startedAt", Timestamp.from(turn.startedAt()))
                    .update();
        } catch (DuplicateKeyException e) {
            throw new TurnInProgressException(turn.conversationId());
        }
    }

    @Override
    public void update(TurnData turn) {
        this.jdbc.sql("""
                        UPDATE turn
                           SET assistant_message = :answer,
                               status = :status,
                               failure_code = :failure,
                               model_calls = :modelCalls,
                               input_tokens = :input,
                               cached_input_tokens = :cached,
                               output_tokens = :output,
                               finished_at = :finishedAt
                         WHERE id = :id
                        """)
                .param("id", turn.id())
                .param("answer", turn.assistantMessage())
                .param("status", turn.status())
                .param("failure", turn.failureCode())
                .param("modelCalls", turn.modelCalls())
                .param("input", turn.inputTokens())
                .param("cached", turn.cachedInputTokens())
                .param("output", turn.outputTokens())
                .param("finishedAt", this.timestamp(turn.finishedAt()))
                .update();
    }

    private Timestamp timestamp(Instant instant) {
        return isNull(instant) ? null : Timestamp.from(instant);
    }

    private TurnData toData(
            ResultSet row,
            int index
    ) throws SQLException {
        Timestamp finished = row.getTimestamp("finished_at");

        return new TurnData(
                row.getObject("id", UUID.class),
                row.getObject("conversation_id", UUID.class),
                row.getObject("user_id", UUID.class),
                row.getObject("client_message_id", UUID.class),
                row.getString("user_message"),
                row.getString("assistant_message"),
                row.getString("status"),
                row.getString("failure_code"),
                row.getString("model"),
                SqlArrays.strings(row.getArray("enabled_contexts")),
                row.getInt("model_calls"),
                row.getLong("input_tokens"),
                row.getLong("cached_input_tokens"),
                row.getLong("output_tokens"),
                row.getTimestamp("started_at").toInstant(),
                isNull(finished) ? null : finished.toInstant()
        );
    }
}
