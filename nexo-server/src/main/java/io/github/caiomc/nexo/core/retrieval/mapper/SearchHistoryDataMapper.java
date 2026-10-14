package io.github.caiomc.nexo.core.retrieval.mapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.catalog.domain.model.BlockedTool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.retrieval.domain.enums.RetrievalStrategy;
import io.github.caiomc.nexo.core.retrieval.domain.model.SearchRecord;
import io.github.caiomc.nexo.core.retrieval.domain.model.SearchRecordResult;
import io.github.caiomc.nexo.core.retrieval.domain.model.SelectedTool;
import io.github.caiomc.nexo.core.retrieval.domain.model.ToolSelection;
import io.github.caiomc.nexo.core.retrieval.port.history.data.ToolSearchData;
import io.github.caiomc.nexo.core.retrieval.port.history.data.ToolSearchResultData;
import io.github.caiomc.nexo.core.retrieval.usecase.command.SelectToolsCommand;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

import static java.util.Objects.isNull;

/**
 * {@link ToolSelection} → linhas de {@code tool_search} e {@code tool_search_result}. As
 * bloqueadas entram depois das entregues, com origem BLOCKED.
 */
@Component
public class SearchHistoryDataMapper {

    private static final String BLOCKED = "BLOCKED";

    public ToolSearchData toData(
            ToolSelection selection,
            SelectToolsCommand command,
            Collection<ContextKey> allowedContexts,
            Instant createdAt
    ) {
        List<ToolSearchResultData> results = new ArrayList<>();

        selection.tools().forEach(tool -> results.add(this.toResult(tool)));
        selection.blocked().forEach(tool -> results.add(this.toResult(results.size() + 1, tool)));

        return new ToolSearchData(
                null,
                isNull(command.userId()) ? null : command.userId().value(),
                command.turnId(),
                selection.query(),
                selection.strategy().name(),
                selection.topK(),
                selection.dagApplied(),
                isNull(allowedContexts) ? List.of() : allowedContexts.stream().map(ContextKey::value).sorted().toList(),
                selection.durationMs(),
                createdAt,
                results
        );
    }

    private ToolSearchResultData toResult(SelectedTool tool) {
        return new ToolSearchResultData(
                tool.step(),
                tool.name().value(),
                tool.origin().name(),
                tool.score(),
                tool.level(),
                List.of()
        );
    }

    private ToolSearchResultData toResult(
            int step,
            BlockedTool tool
    ) {
        return new ToolSearchResultData(
                step,
                tool.name().value(),
                BLOCKED,
                0,
                null,
                tool.missingContexts().stream().map(ContextKey::value).toList()
        );
    }

    public SearchRecord toRecord(ToolSearchData data) {
        return new SearchRecord(
                data.id(),
                data.query(),
                RetrievalStrategy.valueOf(data.strategy()),
                data.topK(),
                data.dagApplied(),
                data.allowedContexts().stream().map(ContextKey::of).toList(),
                data.durationMs(),
                data.createdAt(),
                data.results().stream().map(this::toRecordResult).toList()
        );
    }

    private SearchRecordResult toRecordResult(ToolSearchResultData data) {
        return new SearchRecordResult(
                data.step(),
                ToolName.of(data.toolName()),
                data.origin(),
                data.scoreRag(),
                data.dagLevel(),
                data.missingContexts().stream().map(ContextKey::of).toList()
        );
    }
}
