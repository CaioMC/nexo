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

import io.github.caiomc.nexo.adapters.shared.persistence.JsonColumns;
import io.github.caiomc.nexo.core.conversation.port.repository.ApprovalRepository;
import io.github.caiomc.nexo.core.conversation.port.repository.data.ToolApprovalData;

import static java.util.Objects.isNull;

/**
 * {@code tool_approval}. Toda troca de estado é um {@code UPDATE ... WHERE status = <origem>}: entre
 * duas decisões simultâneas (duas abas, ou decisão e vencimento), só uma vence.
 */
@Component
public class PostgresApprovalRepository implements ApprovalRepository {

    private static final String SELECT = """
            SELECT a.id, a.user_id, t.conversation_id, a.turn_id, a.call_id, a.tool_name, a.description, a.severity,
                   a.arguments::text AS arguments, a.arguments_hash, a.status, a.created_at, a.expires_at, a.decided_at
              FROM tool_approval a
              JOIN turn t ON t.id = a.turn_id
            """;

    private final JdbcClient jdbc;
    private final JsonColumns json;

    public PostgresApprovalRepository(
            JdbcClient jdbc,
            JsonColumns json
    ) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public void insert(ToolApprovalData approval) {
        this.jdbc.sql("""
                        INSERT INTO tool_approval (id, user_id, turn_id, call_id, tool_name, description, severity, arguments, arguments_hash, status, created_at, expires_at)
                        VALUES (:id, :user, :turn, :call, :tool, :description, :severity, CAST(:arguments AS jsonb), :hash, :status, :createdAt, :expiresAt)
                        """)
                .param("id", approval.id())
                .param("user", approval.userId())
                .param("turn", approval.turnId())
                .param("call", approval.callId())
                .param("tool", approval.toolName())
                .param("description", approval.description())
                .param("severity", approval.severity())
                .param("arguments", this.json.write(approval.arguments()))
                .param("hash", approval.argumentsHash())
                .param("status", approval.status())
                .param("createdAt", Timestamp.from(approval.createdAt()))
                .param("expiresAt", Timestamp.from(approval.expiresAt()))
                .update();
    }

    @Override
    public Optional<ToolApprovalData> findById(UUID id) {
        return this.jdbc.sql(SELECT + " WHERE a.id = :id").param("id", id).query(this::toData).optional();
    }

    @Override
    public boolean resolvePending(
            UUID id,
            String to,
            Instant decidedAt
    ) {
        return this.jdbc.sql("UPDATE tool_approval SET status = :to, decided_at = :decidedAt WHERE id = :id AND status = 'PENDING'")
                .param("id", id)
                .param("to", to)
                .param("decidedAt", Timestamp.from(decidedAt))
                .update() == 1;
    }

    @Override
    public boolean consume(
            UUID id,
            String argumentsHash
    ) {
        return this.jdbc.sql("UPDATE tool_approval SET status = 'CONSUMED' WHERE id = :id AND status = 'APPROVED' AND arguments_hash = :hash")
                .param("id", id)
                .param("hash", argumentsHash)
                .update() == 1;
    }

    @Override
    public List<ToolApprovalData> findByConversation(
            UUID conversationId,
            String status
    ) {
        return this.jdbc.sql(SELECT + " WHERE t.conversation_id = :conversation AND (CAST(:status AS text) IS NULL OR a.status = :status) ORDER BY a.created_at")
                .param("conversation", conversationId)
                .param("status", status)
                .query(this::toData)
                .list();
    }

    @Override
    public List<ToolApprovalData> findPendingExpiredAt(Instant now) {
        return this.jdbc.sql(SELECT + " WHERE a.status = 'PENDING' AND a.expires_at <= :now")
                .param("now", Timestamp.from(now))
                .query(this::toData)
                .list();
    }

    @Override
    public List<ToolApprovalData> findPendingByTurn(UUID turnId) {
        return this.jdbc.sql(SELECT + " WHERE a.status = 'PENDING' AND a.turn_id = :turn")
                .param("turn", turnId)
                .query(this::toData)
                .list();
    }

    @Override
    public List<ToolApprovalData> findAllPending() {
        return this.jdbc.sql(SELECT + " WHERE a.status = 'PENDING'").query(this::toData).list();
    }

    private ToolApprovalData toData(
            ResultSet row,
            int index
    ) throws SQLException {
        Timestamp decided = row.getTimestamp("decided_at");

        return new ToolApprovalData(
                row.getObject("id", UUID.class),
                row.getObject("user_id", UUID.class),
                row.getObject("conversation_id", UUID.class),
                row.getObject("turn_id", UUID.class),
                row.getString("call_id"),
                row.getString("tool_name"),
                row.getString("description"),
                row.getString("severity"),
                this.json.read(row.getString("arguments")),
                row.getString("arguments_hash"),
                row.getString("status"),
                row.getTimestamp("created_at").toInstant(),
                row.getTimestamp("expires_at").toInstant(),
                isNull(decided) ? null : decided.toInstant()
        );
    }
}
