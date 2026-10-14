package io.github.caiomc.nexo.adapters.identity.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.adapters.shared.persistence.SqlArrays;
import io.github.caiomc.nexo.core.identity.port.repository.GoogleCredentialRepository;
import io.github.caiomc.nexo.core.identity.port.repository.data.GoogleCredentialData;

/**
 * {@code google_credential}: o único lugar com token do Google, e só o refresh token cifrado.
 * Escopos vão como texto separado por vírgula e viram {@code text[]} no SQL (escopo não tem vírgula).
 */
@Component
public class PostgresGoogleCredentialRepository implements GoogleCredentialRepository {

    private final JdbcClient jdbc;

    public PostgresGoogleCredentialRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<GoogleCredentialData> findByUser(UUID userId) {
        return this.jdbc.sql("SELECT user_id, refresh_token, key_version, granted_scopes, updated_at FROM google_credential WHERE user_id = :user")
                .param("user", userId)
                .query(this::toData)
                .optional();
    }

    @Override
    public void save(GoogleCredentialData credential) {
        this.jdbc.sql("""
                        INSERT INTO google_credential (user_id, refresh_token, key_version, granted_scopes, updated_at)
                        VALUES (:user, :token, :version, string_to_array(:scopes, ','), :updatedAt)
                        ON CONFLICT (user_id) DO UPDATE
                           SET refresh_token = EXCLUDED.refresh_token,
                               key_version = EXCLUDED.key_version,
                               granted_scopes = EXCLUDED.granted_scopes,
                               updated_at = EXCLUDED.updated_at
                        """)
                .param("user", credential.userId())
                .param("token", credential.refreshToken())
                .param("version", credential.keyVersion())
                .param("scopes", String.join(",", credential.grantedScopes()))
                .param("updatedAt", Timestamp.from(credential.updatedAt()))
                .update();
    }

    @Override
    public void delete(UUID userId) {
        this.jdbc.sql("DELETE FROM google_credential WHERE user_id = :user")
                .param("user", userId)
                .update();
    }

    private GoogleCredentialData toData(
            ResultSet row,
            int index
    ) throws SQLException {
        return new GoogleCredentialData(
                row.getObject("user_id", UUID.class),
                row.getBytes("refresh_token"),
                row.getInt("key_version"),
                SqlArrays.strings(row.getArray("granted_scopes")),
                row.getTimestamp("updated_at").toInstant()
        );
    }
}
