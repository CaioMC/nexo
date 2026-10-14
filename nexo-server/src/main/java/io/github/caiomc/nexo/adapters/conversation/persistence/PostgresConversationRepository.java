package io.github.caiomc.nexo.adapters.conversation.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.conversation.port.repository.ConversationRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.data.ConversationData;

import static java.util.Objects.isNull;

@Component
public class PostgresConversationRepository implements ConversationRepository {

    private static final String COLUMNS = "id, user_id, title, last_seq, created_at, updated_at";

    private final JdbcClient jdbc;

    public PostgresConversationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ConversationData> findById(UUID id) {
        return this.jdbc.sql("SELECT " + COLUMNS + " FROM conversation WHERE id = :id")
                .param("id", id)
                .query(this::toData)
                .optional();
    }

    @Override
    public List<ConversationData> findByUser(
            UUID userId,
            Instant before,
            int limit
    ) {
        return this.jdbc.sql("SELECT " + COLUMNS + " FROM conversation WHERE user_id = :user AND (CAST(:before AS timestamptz) IS NULL OR updated_at < :before) ORDER BY updated_at DESC LIMIT :limit")
                .param("user", userId)
                .param("before", isNull(before) ? null : Timestamp.from(before))
                .param("limit", limit)
                .query(this::toData)
                .list();
    }

    @Override
    public void save(ConversationData conversation) {
        this.jdbc.sql("""
                        INSERT INTO conversation (id, user_id, title, last_seq, created_at, updated_at)
                        VALUES (:id, :user, :title, 0, :createdAt, :updatedAt)
                        ON CONFLICT (id) DO UPDATE
                           SET title = EXCLUDED.title,
                               updated_at = EXCLUDED.updated_at
                        """)
                .param("id", conversation.id())
                .param("user", conversation.userId())
                .param("title", conversation.title())
                .param("createdAt", Timestamp.from(conversation.createdAt()))
                .param("updatedAt", Timestamp.from(conversation.updatedAt()))
                .update();
    }

    @Override
    public void delete(UUID id) {
        this.jdbc.sql("DELETE FROM conversation WHERE id = :id").param("id", id).update();
    }

    private ConversationData toData(
            ResultSet row,
            int index
    ) throws SQLException {
        return new ConversationData(
                row.getObject("id", UUID.class),
                row.getObject("user_id", UUID.class),
                row.getString("title"),
                row.getLong("last_seq"),
                row.getTimestamp("created_at").toInstant(),
                row.getTimestamp("updated_at").toInstant()
        );
    }
}
