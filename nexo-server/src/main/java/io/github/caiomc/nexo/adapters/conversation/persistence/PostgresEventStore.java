package io.github.caiomc.nexo.adapters.conversation.persistence;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.github.caiomc.nexo.adapters.shared.persistence.JsonColumns;
import io.github.caiomc.nexo.core.conversation.port.event.EventStorePort;
import io.github.caiomc.nexo.core.conversation.port.event.command.AppendEventCommand;
import io.github.caiomc.nexo.core.conversation.port.event.data.EventData;

/**
 * Event Store em {@code conversation_event}. O {@code seq} sai de {@code conversation.last_seq}
 * (incrementado na mesma transação): crescente e sem buraco por conversa.
 */
@Component
public class PostgresEventStore implements EventStorePort {

    private final JdbcClient jdbc;
    private final JsonColumns json;

    public PostgresEventStore(
            JdbcClient jdbc,
            JsonColumns json
    ) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    @Transactional
    public EventData append(AppendEventCommand command) {
        long seq = this.jdbc.sql("UPDATE conversation SET last_seq = last_seq + 1 WHERE id = :id RETURNING last_seq")
                .param("id", command.conversationId())
                .query(Long.class)
                .single();

        this.jdbc.sql("""
                        INSERT INTO conversation_event (conversation_id, seq, turn_id, type, payload, created_at)
                        VALUES (:conversation, :seq, :turn, :type, CAST(:payload AS jsonb), :at)
                        """)
                .param("conversation", command.conversationId())
                .param("seq", seq)
                .param("turn", command.turnId())
                .param("type", command.type())
                .param("payload", this.json.write(command.payload()))
                .param("at", Timestamp.from(command.at()))
                .update();

        return new EventData(
                command.conversationId(),
                seq,
                command.turnId(),
                command.type(),
                command.payload(),
                command.at()
        );
    }

    @Override
    public List<EventData> findAfter(
            UUID conversationId,
            long afterSeq,
            int limit
    ) {
        return this.jdbc.sql("""
                        SELECT conversation_id, seq, turn_id, type, payload::text AS payload, created_at
                          FROM conversation_event
                         WHERE conversation_id = :conversation AND seq > :after
                         ORDER BY seq
                         LIMIT :limit
                        """)
                .param("conversation", conversationId)
                .param("after", afterSeq)
                .param("limit", limit)
                .query((row, index) -> new EventData(
                        row.getObject("conversation_id", UUID.class),
                        row.getLong("seq"),
                        row.getObject("turn_id", UUID.class),
                        row.getString("type"),
                        this.json.read(row.getString("payload")),
                        row.getTimestamp("created_at").toInstant()
                ))
                .list();
    }
}
