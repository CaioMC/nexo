package io.github.caiomc.nexo.adapters.retrieval.web.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.caiomc.nexo.adapters.identity.security.SessionUser;
import io.github.caiomc.nexo.adapters.retrieval.web.mapper.SearchWebMapper;
import io.github.caiomc.nexo.adapters.retrieval.web.request.SelectToolsRequest;
import io.github.caiomc.nexo.adapters.retrieval.web.response.SearchHistoryResponse;
import io.github.caiomc.nexo.adapters.retrieval.web.response.ToolSelectionResponse;
import io.github.caiomc.nexo.core.retrieval.usecase.ListSearchHistoryUseCase;
import io.github.caiomc.nexo.core.retrieval.usecase.SelectToolsUseCase;

/**
 * {@code POST /api/search}: o pipeline RAG + DAG do agente, exposto para estudo e depuração, com
 * os contextos do usuário logado. {@code GET /api/search/history}: as buscas dele (auditoria).
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SelectToolsUseCase select;
    private final ListSearchHistoryUseCase history;
    private final SearchWebMapper mapper;

    public SearchController(
            SelectToolsUseCase select,
            ListSearchHistoryUseCase history,
            SearchWebMapper mapper
    ) {
        this.select = select;
        this.history = history;
        this.mapper = mapper;
    }

    @PostMapping
    public ToolSelectionResponse select(
            @AuthenticationPrincipal SessionUser user,
            @RequestBody(required = false) SelectToolsRequest request
    ) {
        return this.mapper.toResponse(
                this.select.selectRAGDAG(
                        this.mapper.toCommand(user, request)
                )
        );
    }

    @GetMapping("/history")
    public List<SearchHistoryResponse> history(
            @AuthenticationPrincipal SessionUser user,
            @RequestParam(required = false) Integer limit
    ) {
        return this.mapper.toHistoryResponses(
                this.history.list(
                        this.mapper.toHistoryCommand(user, limit)
                )
        );
    }
}
