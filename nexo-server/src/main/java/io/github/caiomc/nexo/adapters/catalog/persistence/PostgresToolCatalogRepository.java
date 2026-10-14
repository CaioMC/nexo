package io.github.caiomc.nexo.adapters.catalog.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.adapters.shared.persistence.SqlArrays;
import io.github.caiomc.nexo.core.catalog.port.repository.ToolCatalogRepository;
import io.github.caiomc.nexo.core.catalog.port.repository.data.DependencyEdgeData;
import io.github.caiomc.nexo.core.catalog.port.repository.data.ToolData;
import io.github.caiomc.nexo.core.catalog.port.repository.data.ToolParameterData;

import static java.util.Objects.isNull;

/**
 * Leitura crua do catálogo: três {@code SELECT}s, cada linha vira um {@code ...Data}.
 */
@Component
public class PostgresToolCatalogRepository implements ToolCatalogRepository {

    private final JdbcClient jdbc;

    public PostgresToolCatalogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ToolData> tools() {
        return this.jdbc.sql("""
                        SELECT t.name, t.namespace, n.context_key, t.effect, t.description
                        FROM tool t
                        JOIN tool_namespace n ON n.name = t.namespace
                        ORDER BY t.name
                        """)
                .query(this::toTool)
                .list();
    }

    @Override
    public List<ToolParameterData> parameters() {
        return this.jdbc.sql("""
                        SELECT tool_name, ordinal, name, type, description, required, minimum, maximum, enum_values, items::text AS items
                        FROM tool_parameter
                        ORDER BY tool_name, ordinal
                        """)
                .query(this::toParameter)
                .list();
    }

    @Override
    public List<DependencyEdgeData> dependencies() {
        return this.jdbc.sql("SELECT tool_name, depends_on FROM tool_dependency ORDER BY tool_name, depends_on")
                .query((row, number) -> new DependencyEdgeData(row.getString("tool_name"), row.getString("depends_on")))
                .list();
    }

    private ToolData toTool(
            ResultSet row,
            int number
    ) throws SQLException {
        return new ToolData(
                row.getString("name"),
                row.getString("namespace"),
                row.getString("context_key"),
                row.getString("effect"),
                row.getString("description")
        );
    }

    private ToolParameterData toParameter(
            ResultSet row,
            int number
    ) throws SQLException {
        return new ToolParameterData(
                row.getString("tool_name"),
                row.getInt("ordinal"),
                row.getString("name"),
                row.getString("type"),
                row.getString("description"),
                row.getBoolean("required"),
                this.decimal(row.getBigDecimal("minimum")),
                this.decimal(row.getBigDecimal("maximum")),
                SqlArrays.strings(row.getArray("enum_values")),
                row.getString("items")
        );
    }

    private Double decimal(BigDecimal value) {
        return isNull(value) ? null : value.doubleValue();
    }
}
