package io.github.caiomc.nexo.core.retrieval.application;

import java.util.List;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.retrieval.domain.model.SearchRecord;
import io.github.caiomc.nexo.core.retrieval.mapper.SearchHistoryDataMapper;
import io.github.caiomc.nexo.core.retrieval.port.history.SearchHistoryRepository;
import io.github.caiomc.nexo.core.retrieval.usecase.ListSearchHistoryUseCase;
import io.github.caiomc.nexo.core.retrieval.usecase.command.ListSearchHistoryCommand;

/**
 * {@code GET /api/search/history}: as buscas do usuário, com o que cada uma entregou.
 */
@Service
public class SearchHistoryService implements ListSearchHistoryUseCase {

    private final SearchHistoryRepository history;
    private final SearchHistoryDataMapper mapper;

    public SearchHistoryService(
            SearchHistoryRepository history,
            SearchHistoryDataMapper mapper
    ) {
        this.history = history;
        this.mapper = mapper;
    }

    @Override
    public List<SearchRecord> list(ListSearchHistoryCommand command) {
        return this.history.findByUser(command.userId().value(), command.limit())
                .stream()
                .map(this.mapper::toRecord)
                .toList();
    }
}
