package io.github.caiomc.nexo.adapters.retrieval.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.github.caiomc.nexo.adapters.shared.persistence.SqlArrays;
import io.github.caiomc.nexo.core.retrieval.port.history.SearchHistoryRepository;
import io.github.caiomc.nexo.core.retrieval.port.history.data.ToolSearchData;
import io.github.caiomc.nexo.core.retrieval.port.history.data.ToolSearchResultData;

/**
 * Grava cada busca e o que ela entregou. Listas de texto vão como texto separado por vírgula e
 * viram {@code text[]} no SQL ({@code string_to_array}): contexto nunca tem vírgula.
 */
@Component
public class PostgresSearchHistoryRepository implements SearchHistoryRepository {

    private final JdbcClient jdbc;

    public PostgresSearchHistoryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public long save(ToolSearchData search) {
        long id = this.jdbc.sql("""
                        INSERT INTO tool_search (user_id, turn_id, query, strategy, top_k, dag_applied, allowed_contexts, duration_ms, created_at)
                        VALUES (:user, :turn, :query, :strategy, :topK, :dag, string_to_array(:contexts, ','), :duration, :createdAt)
                        RETURNING id
                        """)
                .param("user", search.userId())
                .param("turn", search.turnId())
                .param("query", search.query())
                .param("strategy", search.strategy())
                .param("topK", search.topK())
                .param("dag", search.dagApplied())
                .param("contexts", String.join(",", search.allowedContexts()))
                .param("duration", search.durationMs())
                .param("createdAt", Timestamp.from(search.createdAt()))
                .query(Long.class)
                .single();

        for (ToolSearchResultData result : search.results()) {
            this.saveResult(id, result);
        }

        return id;
    }

    private void saveResult(
            long searchId,
            ToolSearchResultData result
    ) {
        this.jdbc.sql("""
                        INSERT INTO tool_search_result (search_id, step, tool_name, origin, score_rag, dag_level, missing_contexts)
                        VALUES (:search, :step, :tool, :origin, :score, :level, string_to_array(:missing, ','))
                        """)
                .param("search", searchId)
                .param("step", result.step())
                .param("tool", result.toolName())
                .param("origin", result.origin())
                .param("score", result.scoreRag())
                .param("level", result.dagLevel())
                .param("missing", String.join(",", result.missingContexts()))
                .update();
    }

    @Override
    public List<ToolSearchData> findByUser(
            UUID userId,
            int limit
    ) {
        List<ToolSearchData> searches = this.jdbc.sql("""
                        SELECT id, user_id, turn_id, query, strategy, top_k, dag_applied, allowed_contexts, duration_ms, created_at
                          FROM tool_search
                         WHERE user_id = :user
                         ORDER BY created_at DESC, id DESC
                         LIMIT :limit
                        """)
                .param("user", userId)
                .param("limit", limit)
                .query((row, index) -> this.toSearch(row, List.of()))
                .list();

        if (searches.isEmpty()) {
            return searches;
        }

        Map<Long, List<ToolSearchResultData>> results = this.results(searches.stream().map(ToolSearchData::id).toList());

        return searches.stream()
                .map(search -> this.withResults(search, results.getOrDefault(search.id(), List.of())))
                .toList();
    }

    private Map<Long, List<ToolSearchResultData>> results(List<Long> ids) {
        return this.jdbc.sql("""
                        SELECT search_id, step, tool_name, origin, score_rag, dag_level, missing_contexts
                          FROM tool_search_result
                         WHERE search_id = ANY (string_to_array(:ids, ',')::bigint[])
                         ORDER BY search_id, step
                        """)
                .param("ids", ids.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .query((row, index) -> Map.entry(row.getLong("search_id"), this.toResult(row)))
                .list()
                .stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    private ToolSearchData toSearch(
            ResultSet row,
            List<ToolSearchResultData> results
    ) throws SQLException {
        return new ToolSearchData(
                row.getLong("id"),
                row.getObject("user_id", UUID.class),
                row.getObject("turn_id", UUID.class),
                row.getString("query"),
                row.getString("strategy"),
                row.getInt("top_k"),
                row.getBoolean("dag_applied"),
                SqlArrays.strings(row.getArray("allowed_contexts")),
                row.getLong("duration_ms"),
                row.getTimestamp("created_at").toInstant(),
                results
        );
    }

    private ToolSearchResultData toResult(ResultSet row) throws SQLException {
        return new ToolSearchResultData(
                row.getInt("step"),
                row.getString("tool_name"),
                row.getString("origin"),
                row.getDouble("score_rag"),
                (Integer) row.getObject("dag_level"),
                SqlArrays.strings(row.getArray("missing_contexts"))
        );
    }

    private ToolSearchData withResults(
            ToolSearchData search,
            List<ToolSearchResultData> results
    ) {
        return new ToolSearchData(
                search.id(),
                search.userId(),
                search.turnId(),
                search.query(),
                search.strategy(),
                search.topK(),
                search.dagApplied(),
                search.allowedContexts(),
                search.durationMs(),
                search.createdAt(),
                results
        );
    }
}
