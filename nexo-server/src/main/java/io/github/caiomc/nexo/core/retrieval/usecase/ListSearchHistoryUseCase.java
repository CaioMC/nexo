package io.github.caiomc.nexo.core.retrieval.usecase;

import java.util.List;

import io.github.caiomc.nexo.core.retrieval.domain.model.SearchRecord;
import io.github.caiomc.nexo.core.retrieval.usecase.command.ListSearchHistoryCommand;

public interface ListSearchHistoryUseCase {

    List<SearchRecord> list(ListSearchHistoryCommand command);
}
