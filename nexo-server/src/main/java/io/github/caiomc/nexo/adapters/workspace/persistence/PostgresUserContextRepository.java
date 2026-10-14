package io.github.caiomc.nexo.adapters.workspace.persistence;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.workspace.port.repository.UserContextRepository;
import io.github.caiomc.nexo.core.workspace.port.repository.data.UserContextData;

@Component
public class PostgresUserContextRepository implements UserContextRepository {

    private final JdbcClient jdbc;

    public PostgresUserContextRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UserContextData> findByUser(UUID userId) {
        return this.jdbc.sql("SELECT user_id, context_key, enabled, changed_at FROM user_context WHERE user_id = :user")
                .param("user", userId)
                .query((row, index) -> new UserContextData(
                        row.getObject("user_id", UUID.class),
                        row.getString("context_key"),
                        row.getBoolean("enabled"),
                        row.getTimestamp("changed_at").toInstant()
                ))
                .list();
    }

    @Override
    public void save(UserContextData context) {
        this.jdbc.sql("""
                        INSERT INTO user_context (user_id, context_key, enabled, changed_at)
                        VALUES (:user, :context, :enabled, :changedAt)
                        ON CONFLICT (user_id, context_key) DO UPDATE
                           SET enabled = EXCLUDED.enabled,
                               changed_at = EXCLUDED.changed_at
                        """)
                .param("user", context.userId())
                .param("context", context.contextKey())
                .param("enabled", context.enabled())
                .param("changedAt", Timestamp.from(context.changedAt()))
                .update();
    }
}
