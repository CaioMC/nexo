package io.github.caiomc.nexo.adapters.identity.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.identity.port.repository.UserRepository;
import io.github.caiomc.nexo.core.identity.port.repository.data.UserData;

@Component
public class PostgresUserRepository implements UserRepository {

    private static final String COLUMNS = "id, google_subject, email, display_name, picture_url, created_at, last_login_at";

    private final JdbcClient jdbc;

    public PostgresUserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UserData> findById(UUID id) {
        return this.jdbc.sql("SELECT " + COLUMNS + " FROM app_user WHERE id = :id")
                .param("id", id)
                .query(this::toData)
                .optional();
    }

    @Override
    public Optional<UserData> findBySubject(String googleSubject) {
        return this.jdbc.sql("SELECT " + COLUMNS + " FROM app_user WHERE google_subject = :subject")
                .param("subject", googleSubject)
                .query(this::toData)
                .optional();
    }

    @Override
    public void save(UserData user) {
        this.jdbc.sql("""
                        INSERT INTO app_user (id, google_subject, email, display_name, picture_url, created_at, last_login_at)
                        VALUES (:id, :subject, :email, :name, :picture, :createdAt, :lastLoginAt)
                        ON CONFLICT (id) DO UPDATE
                           SET email = EXCLUDED.email,
                               display_name = EXCLUDED.display_name,
                               picture_url = EXCLUDED.picture_url,
                               last_login_at = EXCLUDED.last_login_at
                        """)
                .param("id", user.id())
                .param("subject", user.googleSubject())
                .param("email", user.email())
                .param("name", user.displayName())
                .param("picture", user.pictureUrl())
                .param("createdAt", Timestamp.from(user.createdAt()))
                .param("lastLoginAt", Timestamp.from(user.lastLoginAt()))
                .update();
    }

    private UserData toData(
            ResultSet row,
            int index
    ) throws SQLException {
        return new UserData(
                row.getObject("id", UUID.class),
                row.getString("google_subject"),
                row.getString("email"),
                row.getString("display_name"),
                row.getString("picture_url"),
                row.getTimestamp("created_at").toInstant(),
                row.getTimestamp("last_login_at").toInstant()
        );
    }
}
