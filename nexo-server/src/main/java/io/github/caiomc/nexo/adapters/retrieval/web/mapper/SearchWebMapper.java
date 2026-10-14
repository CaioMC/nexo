package io.github.caiomc.nexo.adapters.retrieval.web.mapper;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.adapters.identity.security.SessionUser;
import io.github.caiomc.nexo.adapters.retrieval.web.request.SelectToolsRequest;
import io.github.caiomc.nexo.adapters.retrieval.web.response.BlockedToolResponse;
import io.github.caiomc.nexo.adapters.retrieval.web.response.SearchHistoryResponse;
import io.github.caiomc.nexo.adapters.retrieval.web.response.SearchHistoryResultResponse;
import io.github.caiomc.nexo.adapters.retrieval.web.response.SelectedToolResponse;
import io.github.caiomc.nexo.adapters.retrieval.web.response.ToolSelectionResponse;
import io.github.caiomc.nexo.core.catalog.domain.model.BlockedTool;
import io.github.caiomc.nexo.core.retrieval.domain.enums.RetrievalStrategy;
import io.github.caiomc.nexo.core.retrieval.domain.model.SearchRecord;
import io.github.caiomc.nexo.core.retrieval.domain.model.SearchRecordResult;
import io.github.caiomc.nexo.core.retrieval.domain.model.SelectedTool;
import io.github.caiomc.nexo.core.retrieval.domain.model.ToolSelection;
import io.github.caiomc.nexo.core.retrieval.usecase.command.ListSearchHistoryCommand;
import io.github.caiomc.nexo.core.retrieval.usecase.command.SelectToolsCommand;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;
import io.github.caiomc.nexo.core.shared.domain.model.UserId;

import static java.util.Objects.isNull;

/**
 * {@link SelectToolsRequest} → {@link SelectToolsCommand} e seleção → resposta. O usuário vem da
 * sessão: a busca filtra pelos contextos que ele deixou ligados.
 */
@Component
public class SearchWebMapper {

    private static final int DEFAULT_HISTORY = 20;

    public SelectToolsCommand toCommand(
            SessionUser user,
            SelectToolsRequest request
    ) {
        if (isNull(request)) {
            throw new IllegalArgumentException("Corpo da requisição ausente: envie {\"query\": \"...\"}");
        }

        return new SelectToolsCommand(
                request.query(),
                this.strategy(request.strategy()),
                request.topK(),
                request.applyDag(),
                UserId.of(user.userId()),
                null
        );
    }

    public ListSearchHistoryCommand toHistoryCommand(
            SessionUser user,
            Integer limit
    ) {
        return new ListSearchHistoryCommand(UserId.of(user.userId()), isNull(limit) ? DEFAULT_HISTORY : limit);
    }

    public List<SearchHistoryResponse> toHistoryResponses(List<SearchRecord> records) {
        return records.stream().map(this::toHistoryResponse).toList();
    }

    public ToolSelectionResponse toResponse(ToolSelection selection) {
        return new ToolSelectionResponse(
                selection.searchId(),
                selection.query(),
                selection.strategy().name(),
                selection.topK(),
                selection.dagApplied(),
                selection.durationMs(),
                selection.tools().stream().map(this::toResponse).toList(),
                selection.blocked().stream().map(this::toResponse).toList()
        );
    }

    private RetrievalStrategy strategy(String value) {
        if (isNull(value) || value.isBlank()) {
            return null;
        }

        try {
            return RetrievalStrategy.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estratégia desconhecida: '" + value + "'. Use FULL_TEXT, VECTOR ou HYBRID.");
        }
    }

    private SearchHistoryResponse toHistoryResponse(SearchRecord record) {
        return new SearchHistoryResponse(
                record.id(),
                record.query(),
                record.strategy().name(),
                record.topK(),
                record.dagApplied(),
                record.allowedContexts().stream().map(ContextKey::value).toList(),
                record.durationMs(),
                record.createdAt(),
                record.results().stream().map(this::toHistoryResult).toList()
        );
    }

    private SearchHistoryResultResponse toHistoryResult(SearchRecordResult result) {
        return new SearchHistoryResultResponse(
                result.step(),
                result.tool().value(),
                result.origin(),
                result.score(),
                result.level(),
                result.missingContexts().stream().map(ContextKey::value).toList()
        );
    }

    private SelectedToolResponse toResponse(SelectedTool tool) {
        return new SelectedToolResponse(
                tool.step(),
                tool.name().value(),
                tool.origin().name(),
                tool.score(),
                tool.level()
        );
    }

    private BlockedToolResponse toResponse(BlockedTool tool) {
        return new BlockedToolResponse(
                tool.name().value(),
                tool.missingContexts().stream().map(ContextKey::value).toList()
        );
    }
}
