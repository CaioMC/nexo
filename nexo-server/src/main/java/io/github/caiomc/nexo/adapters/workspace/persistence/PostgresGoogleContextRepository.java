package io.github.caiomc.nexo.adapters.workspace.persistence;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.workspace.port.repository.GoogleContextRepository;
import io.github.caiomc.nexo.core.workspace.port.repository.data.GoogleContextData;
import io.github.caiomc.nexo.core.workspace.port.repository.data.GoogleContextScopeData;

@Component
public class PostgresGoogleContextRepository implements GoogleContextRepository {

    private final JdbcClient jdbc;

    public PostgresGoogleContextRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<GoogleContextData> contexts() {
        return this.jdbc.sql("SELECT key, name, description, google_api, position FROM google_context ORDER BY position")
                .query((row, index) -> new GoogleContextData(
                        row.getString("key"),
                        row.getString("name"),
                        row.getString("description"),
                        row.getString("google_api"),
                        row.getInt("position")
                ))
                .list();
    }

    @Override
    public List<GoogleContextScopeData> scopes() {
        return this.jdbc.sql("SELECT context_key, scope FROM google_context_scope ORDER BY context_key, scope")
                .query((row, index) -> new GoogleContextScopeData(row.getString("context_key"), row.getString("scope")))
                .list();
    }
}
